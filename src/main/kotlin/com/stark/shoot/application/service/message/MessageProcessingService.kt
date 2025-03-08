package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.SaveChatMessagePort
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.domain.chat.room.Participant
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.types.ObjectId
import org.springframework.data.redis.core.StringRedisTemplate

@UseCase
class MessageProcessingService(
    private val saveChatMessagePort: SaveChatMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val eventPublisher: EventPublisher,
    private val redisTemplate: StringRedisTemplate
) : ProcessMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 저장 및 채팅방 메타데이터 업데이트 담당
     * 새 메시지가 도착할 때 sender를 제외한 다른 참여자의 unreadCount가 증가하고,
     * 사용자가 해당 채팅방을 열거나 스크롤하여 메시지를 확인하면(예: "모두 읽음" 버튼 또는 자동 감지), unreadCount가 0으로 업데이트되도록 하면 됩니다.
     */
    override fun processMessageCreate(
        message: ChatMessage
    ): ChatMessage {
        // 채팅방 조회 (존재하지 않으면 예외 발생)
        val roomObjectId = message.roomId.toObjectId()
        val chatRoom = loadChatRoomPort.findById(roomObjectId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=${message.roomId}")

        // readBy 초기화 후 저장
        val savedMessage = saveChatMessage(message, chatRoom)

        // 보낸 사람 제외 unreadCount 증가
        val updatedParticipants = updateUnreadCount(message, chatRoom)

        // 채팅방 업데이트
        updateChatRoom(chatRoom, updatedParticipants, savedMessage)

        // unreadCount 정보 추출
        val unreadCounts: Map<String, Int> = updatedParticipants.mapKeys { it.key.toString() }
            .mapValues { it.value.unreadCount }

        // unreadCount와 마지막 메시지 정보 이벤트 발행 (채팅방 목록 업데이트용)
        publishMessageCreatedEvent(roomObjectId, unreadCounts, savedMessage)

        return savedMessage
    }

    /**
     * readBy 초기화 후 메시지 저장
     *
     * @param message 채팅 메시지
     * @param chatRoom 채팅방
     * @return ChatMessage 저장된 메시지
     */
    private fun saveChatMessage(
        message: ChatMessage,
        chatRoom: ChatRoom
    ): ChatMessage {
        // readBy 초기화 (메시지에 readBy 추가 → 발신자는 읽음(true), 나머지는 안 읽음(false).)
        val initializedMessage = message.copy(
            readBy = chatRoom.metadata.participantsMetadata.keys.associate {
                it.toString() to (it == ObjectId(message.senderId))
            }.toMutableMap()
        )

        // 초기화된 메시지를 DB에 저장
        val savedMessage = saveChatMessagePort.save(initializedMessage)

        return savedMessage
    }

    /**
     * 발신자를 제외한 참여자의 unreadCount 증가
     *
     * @param message 채팅 메시지
     * @param chatRoom 채팅방
     * @return Map<ObjectId, Participant> 업데이트된 참여자 목록
     */
    private fun updateUnreadCount(
        message: ChatMessage,
        chatRoom: ChatRoom
    ): Map<ObjectId, Participant> {
        // 발신자 ObjectId 추출
        val senderObjectId = ObjectId(message.senderId)

        // 참여자 목록을 순회하면서 unreadCount 증가
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

        return updatedParticipants
    }

    /**
     * 채팅방 업데이트
     *
     * @param chatRoom 채팅방
     * @param updatedParticipants 업데이트된 참여자 목록
     * @param savedMessage 저장된 메시지
     */
    private fun updateChatRoom(
        chatRoom: ChatRoom,
        updatedParticipants: Map<ObjectId, Participant>,
        savedMessage: ChatMessage
    ) {
        // 채팅방 업데이트
        val updatedRoom = chatRoom.copy(
            metadata = chatRoom.metadata.copy(participantsMetadata = updatedParticipants),
            lastMessageId = savedMessage.id,
            lastMessageText = savedMessage.content.text // 이 필드를 추가해서 lastMessage 내용 저장
        )

        saveChatRoomPort.save(updatedRoom)
    }

    /**
     * unreadCount와 마지막 메시지 정보 이벤트 발행
     *
     * @param roomObjectId 채팅방 ObjectId
     * @param unreadCounts 참여자별 unreadCount
     * @param savedMessage 저장된 메시지
     */
    private fun publishMessageCreatedEvent(
        roomObjectId: ObjectId,
        unreadCounts: Map<String, Int>,
        savedMessage: ChatMessage
    ) {
        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = roomObjectId.toString(),
                unreadCounts = unreadCounts,
                lastMessage = savedMessage.content.text
            )
        )
    }

}
