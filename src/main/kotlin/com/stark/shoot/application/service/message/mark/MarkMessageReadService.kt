package com.stark.shoot.application.service.message.mark

import com.stark.shoot.application.port.`in`.message.mark.MarkMessageReadUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
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
import org.bson.types.ObjectId
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant
import java.util.concurrent.TimeUnit

@UseCase
class MarkMessageReadService(
    private val saveMessagePort: SaveMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val eventPublisher: EventPublisher,
    private val redisTemplate: StringRedisTemplate,
    private val loadMessagePort: LoadMessagePort,
    private val simpMessagingTemplate: SimpMessagingTemplate
) : MarkMessageReadUseCase {
    private val logger = KotlinLogging.logger {}

    override fun markMessageAsRead(messageId: String, userId: String): ChatMessage {
        val chatMessage = loadMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$messageId")

        // 업데이트 전 안 읽은 메시지 가져오기
        val roomId = chatMessage.roomId.toObjectId()
        val userObjectId = ObjectId(userId)
        val unreadMessagesBefore = loadMessagePort.findUnreadByRoomId(roomId, userObjectId)
        logger.debug { "Unread messages before marking as read: $unreadMessagesBefore" }

        // 읽음 상태 업데이트
        chatMessage.readBy[userId] = true
        val updatedMessage = saveMessagePort.save(chatMessage)

        // unreadCount 계산
        val unreadCountBefore = unreadMessagesBefore.size ?: 0
        val wasUnread = unreadMessagesBefore.any { it.id == messageId }
        val newUnreadCount = if (wasUnread) unreadCountBefore - 1 else unreadCountBefore
        val finalUnreadCount = newUnreadCount.coerceAtLeast(0)

        // 채팅방 정보 업데이트
        val room = loadChatRoomPort.findById(roomId)!!
        val participant = room.metadata.participantsMetadata[userObjectId]!!
        val updatedParticipant = participant.copy(unreadCount = finalUnreadCount, lastReadAt = Instant.now())
        val updatedParticipants =
            room.metadata.participantsMetadata.toMutableMap().apply { put(userObjectId, updatedParticipant) }
        val updatedRoom = room.copy(metadata = room.metadata.copy(participantsMetadata = updatedParticipants))
        saveChatRoomPort.save(updatedRoom)

        // Redis와 이벤트 업데이트
        redisTemplate.opsForHash<String, String>().put("unread:$userId", roomId.toString(), finalUnreadCount.toString())
        val unreadCounts = updatedParticipants.mapKeys { it.key.toString() }.mapValues { it.value.unreadCount }
        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = roomId.toString(),
                unreadCounts = unreadCounts,
                lastMessage = updatedMessage.content.text
            )
        )

        logger.info { "Marked message as read: messageId=$messageId, userId=$userId, newUnreadCount=$finalUnreadCount" }
        return updatedMessage
    }

    // MessageProcessingService.kt 수정
    override fun markAllMessagesAsRead(roomId: String, userId: String, requestId: String?) {
        val roomObjectId = roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=$roomId")
        val participantId = ObjectId(userId)
        val participantMeta = chatRoom.metadata.participantsMetadata[participantId]
            ?: throw IllegalArgumentException("참여자가 없습니다. userId=$userId")

        // 중복 요청 체크 (Redis 캐시 활용)
        if (!requestId.isNullOrEmpty()) {
            val key = "read_operation:$roomId:$userId:$requestId"
            val exists = redisTemplate.opsForValue().get(key)
            if (exists != null) {
                logger.info { "중복 읽음 처리 요청 감지 (requestId=$requestId)" }
                return
            }
            // 요청 정보 캐싱 (5초 만료)
            redisTemplate.opsForValue().set(key, "1", 5, TimeUnit.SECONDS)
        }

        // 모든 안 읽은 메시지 읽음 처리
        val unreadMessages = loadMessagePort.findUnreadByRoomId(roomObjectId, participantId)
            .filter { it.senderId != userId }  // 내가 보낸 메시지는 제외

        // 안 읽은 메시지가 없으면 불필요한 처리와 이벤트 발행 방지
        if (unreadMessages.isEmpty()) {
            logger.info { "읽지 않은 메시지가 없습니다. roomId=$roomId, userId=$userId" }
            return
        }

        val updatedMessageIds = mutableListOf<String>()
        unreadMessages.forEach { message ->
            message.readBy[userId] = true
            val updated = saveMessagePort.save(message)
            updatedMessageIds.add(updated.id!!)
        }

        // 참여자 unreadCount 초기화
        val updatedParticipant = participantMeta.copy(unreadCount = 0, lastReadAt = Instant.now())
        val updatedParticipants = chatRoom.metadata.participantsMetadata.toMutableMap()
            .apply { put(participantId, updatedParticipant) }
        val updatedRoom = chatRoom.copy(metadata = chatRoom.metadata.copy(participantsMetadata = updatedParticipants))
        saveChatRoomPort.save(updatedRoom)

        // Redis와 이벤트 업데이트 (lastMessage 필드 추가 - null 방지)
        redisTemplate.opsForHash<String, String>().put("unread:$userId", roomId, "0")
        val unreadCounts = updatedParticipants.mapKeys { it.key.toString() }.mapValues { it.value.unreadCount }

        // 마지막 메시지 찾기 (null 방지)
        val lastMessage = chatRoom.lastMessageText ?: "최근 메시지가 없습니다."

        // 이벤트 발행 시 lastMessage 필드에 의미 있는 값 전달
        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = roomId,
                unreadCounts = unreadCounts,
                lastMessage = lastMessage
            )
        )

        // 여기서 Bulk Read 이벤트 발행
        if (updatedMessageIds.isNotEmpty()) {
            simpMessagingTemplate.convertAndSend(
                "/topic/read-bulk/$roomId",
                ChatBulkReadEvent(roomId, updatedMessageIds, userId)
            )
        }

        logger.info { "Marked all messages as read: roomId=$roomId, userId=$userId" }
    }

}
