package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.port.out.*
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.common.exception.ResourceNotFoundException
import com.stark.shoot.infrastructure.common.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.types.ObjectId
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MessageProcessingService(
    private val saveChatMessagePort: SaveChatMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val eventPublisher: EventPublisher,
    private val redisTemplate: StringRedisTemplate,
    private val loadChatMessagePort: LoadChatMessagePort
) : ProcessMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 저장 및 채팅방 메타데이터 업데이트 담당
     * 새 메시지가 도착할 때 sender를 제외한 다른 참여자의 unreadCount가 증가하고,
     * 사용자가 해당 채팅방을 열거나 스크롤하여 메시지를 확인하면(예: "모두 읽음" 버튼 또는 자동 감지), unreadCount가 0으로 업데이트되도록 하면 됩니다.
     */
    override fun processMessage(message: ChatMessage): ChatMessage {
        val roomObjectId = message.roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        logger.debug { "Participants: ${chatRoom.metadata.participantsMetadata.keys}" }

        // readBy 초기화 (메시지에 readBy 추가 → 발신자는 읽음(true), 나머지는 안 읽음(false).)
        val initializedMessage = message.copy(
            readBy = chatRoom.metadata.participantsMetadata.keys.associate {
                it.toString() to (it == ObjectId(message.senderId))
            }.toMutableMap()
        )

        logger.debug { "Initialized readBy: ${initializedMessage.readBy}" }

        // 초기화된 메시지를 DB에 저장
        val savedMessage = saveChatMessagePort.save(initializedMessage)

        // 보낸 사람 제외 unreadCount 증가
        val senderObjectId = ObjectId(message.senderId)
        val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (participantId, participant) ->
            // Redis에서 참여자가 방에 있는지(active) 확인. 없으면 false로 간주.
            val isActive = redisTemplate.opsForValue()
                .get("active:$participantId:${message.roomId}")?.toBoolean() ?: false

            // 발신자가 아니고 방에 없으면 unreadCount 1 증가.
            if (participantId != senderObjectId && !isActive) {
                participant.copy(unreadCount = participant.unreadCount + 1)
            } else {
                participant
            }
        }

        // 채팅방 업데이트 (unreadCount 갱신과 마지막 메시지 ID 저장.)
        val updatedRoom = chatRoom.copy(
            metadata = chatRoom.metadata.copy(participantsMetadata = updatedParticipants),
            lastMessageId = savedMessage.id
        )
        saveChatRoomPort.save(updatedRoom)

        // Redis에 unreadCount 캐싱 (사용자별 Hash : unread:<userId> 키에 roomId와 unreadCount를 Hash로 저장.)
        // 역할: 사용자가 자신의 모든 채팅방에 대한 unreadCount를 빠르게 조회 가능. MongoDB 대신 Redis에서 읽어 부하 감소.
        updatedParticipants.forEach { (userId, participant) ->
            redisTemplate.opsForHash<String, String>().put(
                "unread:$userId",
                message.roomId,
                participant.unreadCount.toString()
            )
        }

        // unreadCount 정보 추출
        val unreadCounts: Map<String, Int> = updatedParticipants.mapKeys { it.key.toString() }
            .mapValues { it.value.unreadCount }

        // unreadCount와 마지막 메시지 정보 발행 → SSE로 ChatRoomList에 반영.
        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = roomObjectId.toString(),
                unreadCounts = unreadCounts,
                lastMessage = savedMessage.content.text
            )
        )

        return savedMessage
    }

    override fun markMessageAsRead(messageId: String, userId: String): ChatMessage {
        val chatMessage = loadChatMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$messageId")

        // 업데이트 전 안 읽은 메시지 가져오기
        val roomId = chatMessage.roomId.toObjectId()
        val userObjectId = ObjectId(userId)
        val unreadMessagesBefore = loadChatMessagePort.findUnreadByRoomId(roomId, userObjectId)
        logger.debug { "Unread messages before marking as read: $unreadMessagesBefore" }

        // 읽음 상태 업데이트
        chatMessage.readBy[userId] = true
        val updatedMessage = saveChatMessagePort.save(chatMessage)

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

    override fun markAllMessagesAsRead(roomId: String, userId: String) {
        val roomObjectId = roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=$roomId")
        val participantId = ObjectId(userId)
        val participantMeta = chatRoom.metadata.participantsMetadata[participantId]
            ?: throw IllegalArgumentException("참여자가 없습니다. userId=$userId")

        // 모든 안 읽은 메시지 읽음 처리
        val unreadMessages = loadChatMessagePort.findUnreadByRoomId(roomObjectId, participantId)
        unreadMessages.forEach { message ->
            message.readBy[userId] = true
            saveChatMessagePort.save(message)
        }

        // 참여자 unreadCount 초기화
        val updatedParticipant = participantMeta.copy(unreadCount = 0, lastReadAt = Instant.now())
        val updatedParticipants = chatRoom.metadata.participantsMetadata.toMutableMap()
            .apply { put(participantId, updatedParticipant) }
        val updatedRoom = chatRoom.copy(metadata = chatRoom.metadata.copy(participantsMetadata = updatedParticipants))
        saveChatRoomPort.save(updatedRoom)

        // Redis와 이벤트 업데이트
        redisTemplate.opsForHash<String, String>().put("unread:$userId", roomId, "0")
        val unreadCounts = updatedParticipants.mapKeys { it.key.toString() }.mapValues { it.value.unreadCount }
        eventPublisher.publish(ChatUnreadCountUpdatedEvent(roomId = roomId, unreadCounts = unreadCounts))

        logger.info { "Marked all messages as read: roomId=$roomId, userId=$userId" }
    }

}
