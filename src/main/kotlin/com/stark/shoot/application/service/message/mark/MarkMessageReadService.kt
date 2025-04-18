package com.stark.shoot.application.service.message.mark

import com.stark.shoot.application.port.`in`.message.mark.MarkMessageReadUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.ReadStatusPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.event.ChatBulkReadEvent
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Transactional
@UseCase
class MarkMessageReadService(
    private val saveMessagePort: SaveMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val readStatusPort: ReadStatusPort,
    private val eventPublisher: EventPublisher,
    private val redisTemplate: StringRedisTemplate,
    private val loadMessagePort: LoadMessagePort,
    private val simpMessagingTemplate: SimpMessagingTemplate
) : MarkMessageReadUseCase {
    private val logger = KotlinLogging.logger {}

    /**
     * 단일 메시지를 읽음 상태로 표시합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 업데이트된 메시지
     */
    override fun markMessageAsRead(
        messageId: String,
        userId: Long
    ): ChatMessage {
        val chatMessage = loadMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$messageId")

        // 이미 읽은 메시지인 경우 추가 작업 없이 반환
        if (chatMessage.readBy[userId] == true) {
            return chatMessage
        }

        // 메시지 읽음 상태 업데이트
        chatMessage.readBy[userId] = true
        val updatedMessage = saveMessagePort.save(chatMessage)

        // 채팅방 사용자의 마지막 읽은 메시지 ID 업데이트
        val roomId = chatMessage.roomId.toLong()
        val userIdLong = userId.toLong()

        readStatusPort.updateLastReadMessageId(roomId, userIdLong, messageId)

        // 읽지 않은 메시지 수 업데이트 (Redis 활용)
        updateUnreadCountInRedis(roomId, userId)

        // 이벤트 발행 (채팅방의 모든 참여자에게 읽은 상태 알림)
        publishReadEvent(roomId, chatMessage)

        logger.info { "메시지 읽음 처리 완료: messageId=$messageId, userId=$userId" }
        return updatedMessage
    }

    /**
     * 채팅방의 모든 메시지를 읽음 상태로 표시합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param requestId 요청 ID (중복 요청 방지용, 선택적)
     */
    override fun markAllMessagesAsRead(
        roomId: Long,
        userId: Long,
        requestId: Long?
    ) {
        // 중복 요청 체크 (Redis 캐시 활용)
        if (requestId != null) {
            val key = "read_operation:$roomId:$userId:$requestId"
            val exists = redisTemplate.opsForValue().get(key)
            if (exists != null) {
                logger.info { "중복 읽음 처리 요청 감지 (requestId=$requestId)" }
                return
            }
            // 요청 정보 캐싱 (5초 만료)
            redisTemplate.opsForValue().set(key, "1", 5, TimeUnit.SECONDS)
        }

        // 채팅방 조회 및 사용자 참여 확인
        val chatRoom = loadChatRoomPort.findById(roomId.toLong())
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=$roomId")

        if (!chatRoom.participants.contains(userId.toLong())) {
            throw IllegalArgumentException("참여하지 않은 채팅방입니다. userId=$userId, roomId=$roomId")
        }

        // 읽지 않은 메시지 조회 (내가 보낸 메시지 제외)
        val unreadMessages = loadMessagePort.findUnreadByRoomId(roomId, userId).filter { it.senderId != userId }

        // 읽지 않은 메시지가 없으면 불필요한 작업 방지
        if (unreadMessages.isEmpty()) {
            logger.info { "읽지 않은 메시지가 없습니다. roomId=$roomId, userId=$userId" }
            return
        }

        // 모든 메시지 읽음 처리
        val updatedMessageIds = processUnreadMessages(unreadMessages, userId)

        // 마지막으로 읽은 메시지 ID 업데이트
        val lastMessage = unreadMessages.maxByOrNull { it.createdAt ?: java.time.Instant.MIN }?.id
        if (lastMessage != null) {
            readStatusPort.updateLastReadMessageId(roomId, userId, lastMessage)
        }

        // Redis와 이벤트 업데이트
        redisTemplate.opsForHash<String, String>().put("unread:$userId", roomId.toString(), "0")

        // 채팅방의 마지막 메시지 내용 조회
        val lastMessageText = chatRoom.lastMessageId?.let {
            // 실제 구현시 메시지 내용 조회 로직 추가
            "최근 메시지"
        } ?: "메시지가 없습니다"

        // 이벤트 발행
        publishBulkReadEvent(roomId, updatedMessageIds, userId, lastMessageText)

        logger.info { "채팅방 모든 메시지 읽음 처리 완료: roomId=$roomId, userId=$userId, 메시지 수=${updatedMessageIds.size}" }
    }

    /**
     * 읽지 않은 메시지들을 일괄 처리합니다.
     *
     * @param unreadMessages 읽지 않은 메시지 목록
     * @param userId 사용자 ID
     * @return 업데이트된 메시지 ID 목록
     */
    private fun processUnreadMessages(
        unreadMessages: List<ChatMessage>,
        userId: Long
    ): List<String> {
        val updatedMessageIds = mutableListOf<String>()

        unreadMessages.forEach { message ->
            message.readBy[userId] = true
            val updated = saveMessagePort.save(message)
            updated.id?.let { updatedMessageIds.add(it) }
        }

        return updatedMessageIds
    }

    /**
     * Redis에 읽지 않은 메시지 수를 업데이트합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    private fun updateUnreadCountInRedis(
        roomId: Long,
        userId: Long
    ) {
        try {
            // 현재 읽지 않은 메시지 수 조회
            val currentUnreadCount = redisTemplate.opsForHash<String, String>()
                .get("unread:$userId", roomId)?.toIntOrNull() ?: 0

            // 읽지 않은 메시지 수가 0보다 크면 감소
            if (currentUnreadCount > 0) {
                val newUnreadCount = currentUnreadCount - 1
                redisTemplate.opsForHash<String, String>()
                    .put("unread:$userId", roomId.toString(), newUnreadCount.toString())
            }
        } catch (e: Exception) {
            logger.error(e) { "Redis 읽지 않은 메시지 수 업데이트 실패: roomId=$roomId, userId=$userId" }
        }
    }

    /**
     * 메시지 읽음 이벤트를 발행합니다.
     *
     * @param roomId 채팅방 ID
     * @param message 읽은 메시지
     */
    private fun publishReadEvent(
        roomId: Long,
        message: ChatMessage
    ) {
        // 채팅방의 모든 참여자에게 읽음 상태 업데이트를 알림
        simpMessagingTemplate.convertAndSend(
            "/topic/read/$roomId",
            mapOf(
                "messageId" to message.id,
                "userId" to message.senderId,
                "readBy" to message.readBy
            )
        )
    }

    /**
     * 일괄 메시지 읽음 이벤트를 발행합니다.
     *
     * @param roomId 채팅방 ID
     * @param messageIds 읽은 메시지 ID 목록
     * @param userId 사용자 ID
     * @param lastMessage 마지막 메시지 내용
     */
    private fun publishBulkReadEvent(
        roomId: Long,
        messageIds: List<String>,
        userId: Long,
        lastMessage: String
    ) {
        // 채팅방 일괄 읽음 이벤트 발행
        if (messageIds.isNotEmpty()) {
            // WebSocket을 통한 실시간 알림
            simpMessagingTemplate.convertAndSend(
                "/topic/read-bulk/$roomId",
                ChatBulkReadEvent(roomId, messageIds, userId)
            )

            // 읽지 않은 메시지 수 업데이트 이벤트 발행
            eventPublisher.publish(
                ChatUnreadCountUpdatedEvent(
                    roomId = roomId,
                    unreadCounts = mapOf(userId to 0),
                    lastMessage = lastMessage
                )
            )
        }
    }

}