package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageType
import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.MessageMetadata
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.Participant
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.redis.RedisLockManager
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bson.types.ObjectId
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.*

@UseCase
class MessageProcessingService(
    private val saveMessagePort: SaveMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val eventPublisher: EventPublisher,
    private val redisTemplate: StringRedisTemplate,
    private val redisLockManager: RedisLockManager,
    private val extractUrlPort: ExtractUrlPort,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort
) : ProcessMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 저장 및 채팅방 메타데이터 업데이트 담당
     *
     * @param message 채팅 메시지
     * @return ChatMessage 저장된 메시지
     */
    override suspend fun processMessageCreate(
        message: ChatMessage
    ): ChatMessage {
        // 분산 락 키 생성 (채팅방별로 락을 걸기 위해 사용)
        val lockKey = "chatroom:${message.roomId}"
        val ownerId = "processor-${UUID.randomUUID()}"
        val startTime = System.currentTimeMillis()

        try {
            // 코루틴 블록 내에서 분산 락 획득
            return redisLockManager.withLockSuspend(lockKey, ownerId) {
                processMessageWithPipeline(message)
            }
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 오류: ${message.id}" }
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logger.debug { "메시지 처리 소요 시간: ${duration}ms, messageId: ${message.id}" }
        }
    }

    /**
     * 메시지 처리 파이프라인 구현
     * 1. URL 처리
     * 2. 채팅방 조회 및 메시지 저장
     * 3. Unread Count 업데이트
     * 4. 채팅방 메타데이터 업데이트
     * 5. 이벤트 발행
     */
    private suspend fun processMessageWithPipeline(
        message: ChatMessage
    ): ChatMessage = coroutineScope {
        // 1. 채팅방 조회 (존재하지 않으면 예외 발생)
        val roomObjectId = message.roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // 2. URL 처리와 unreadCount 업데이트를 병렬로 처리
        val processUrlDeferred = async { processUrl(message) }
        val updatedParticipantsDeferred = async { updateUnreadCount(message, chatRoom) }

        // 3. 병렬 작업 결과 수집
        val processedMessage = processUrlDeferred.await()
        val updatedParticipants = updatedParticipantsDeferred.await()

        // 4. readBy 필드 초기화 및 메시지 저장
        val initializedMessage = initializeReadBy(processedMessage, chatRoom)
        val savedMessage = saveMessagePort.save(initializedMessage)

        // 5. 채팅방 메타데이터 업데이트 및 저장
        val updatedRoom = updateChatRoom(chatRoom, updatedParticipants, savedMessage)
        saveChatRoomPort.save(updatedRoom)

        // 6. 이벤트 발행
        publishUnreadCountEvent(updatedRoom, savedMessage)

        // 7. 처리된 메시지 반환
        savedMessage
    }

    /**
     * URL 미리보기 처리
     */
    private suspend fun processUrl(
        message: ChatMessage
    ): ChatMessage {
        // 텍스트 메시지가 아니면 그대로 반환
        if (message.content.type != MessageType.TEXT) {
            return message
        }

        // URL 추출
        val urls = extractUrlPort.extractUrls(message.content.text)
        if (urls.isEmpty()) {
            return message
        }

        // 첫 번째 URL만 처리
        val url = urls.first()

        // 캐시된 미리보기 확인
        val preview = cacheUrlPreviewPort.getCachedUrlPreview(url)
            ?: loadUrlContentPort.fetchUrlContent(url)?.also {
                // 캐시에 저장
                cacheUrlPreviewPort.cacheUrlPreview(url, it)
            }

        // 미리보기 정보 있으면 메시지에 추가
        return if (preview != null) {
            val currentMetadata = message.content.metadata ?: MessageMetadata()
            val updatedMetadata = currentMetadata.copy(urlPreview = preview)
            val updatedContent = message.content.copy(metadata = updatedMetadata)
            message.copy(content = updatedContent)
        } else {
            message
        }
    }

    /**
     * readBy 필드 초기화 (발신자는 읽음, 나머지는 안읽음)
     */
    private fun initializeReadBy(
        message: ChatMessage,
        chatRoom: ChatRoom
    ): ChatMessage {
        val readBy = chatRoom.metadata.participantsMetadata.keys.associate {
            it.toString() to (it == ObjectId(message.senderId))
        }.toMutableMap()

        return message.copy(readBy = readBy)
    }

    /**
     * 발신자를 제외한 참여자의 unreadCount 증가
     */
    private suspend fun updateUnreadCount(
        message: ChatMessage,
        chatRoom: ChatRoom
    ): Map<ObjectId, Participant> {
        val senderObjectId = ObjectId(message.senderId)

        return chatRoom.metadata.participantsMetadata.mapValues { (participantId, participant) ->
            // Redis에서 참여자가 방에 있는지(active) 확인
            val isActive = redisTemplate.opsForValue()
                .get("active:$participantId:${message.roomId}")?.toBoolean() ?: false

            // 발신자가 아니고 방에 없으면 unreadCount 증가
            if (participantId != senderObjectId && !isActive) {
                participant.copy(unreadCount = participant.unreadCount + 1)
            } else {
                participant
            }
        }
    }

    /**
     * 채팅방 메타데이터 업데이트
     */
    private fun updateChatRoom(
        chatRoom: ChatRoom,
        updatedParticipants: Map<ObjectId, Participant>,
        message: ChatMessage
    ): ChatRoom {
        return chatRoom.copy(
            metadata = chatRoom.metadata.copy(participantsMetadata = updatedParticipants),
            lastMessageId = message.id,
            lastMessageText = message.content.text
        )
    }

    /**
     * 읽지 않은 메시지 수 이벤트 발행
     */
    private fun publishUnreadCountEvent(
        chatRoom: ChatRoom,
        message: ChatMessage
    ) {
        val unreadCounts = chatRoom.metadata.participantsMetadata.mapKeys { it.key.toString() }
            .mapValues { it.value.unreadCount }

        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = chatRoom.id.toString(),
                unreadCounts = unreadCounts,
                lastMessage = message.content.text
            )
        )
    }

}