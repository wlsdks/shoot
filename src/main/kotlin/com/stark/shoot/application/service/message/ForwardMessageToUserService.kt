package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.ForwardMessageToUserUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageForwardDomainService
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.domain.chatroom.service.ChatRoomEventService
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class ForwardMessageToUserService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort,
    private val findUserPort: FindUserPort,
    private val eventPublisher: EventPublisher,
    private val chatRoomEventService: ChatRoomEventService,
    private val chatRoomDomainService: ChatRoomDomainService,
    private val messageForwardDomainService: MessageForwardDomainService,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService
) : ForwardMessageToUserUseCase {

    /**
     * 메시지를 특정 사용자(친구)에게 전달합니다.
     * 1:1 채팅방을 생성하거나 조회한 후, 해당 채팅방으로 메시지를 전달합니다.
     */
    override fun forwardMessageToUser(
        originalMessageId: MessageId,
        targetUserId: UserId,
        forwardingUserId: UserId
    ): ChatMessage {
        // 1. 사용자 간 1:1 채팅방 조회 또는 생성
        val chatRoom = findOrCreateDirectChat(forwardingUserId, targetUserId)

        val roomId = chatRoom.id?.value
            ?: throw IllegalStateException("채팅방 ID가 null입니다.")

        // 2. 해당 채팅방으로 메시지 전달
        val forwardedMessage = forwardMessage(
            originalMessageId = originalMessageId,
            targetRoomId = ChatRoomId.from(roomId),
            forwardingUserId = forwardingUserId
        )

        return forwardedMessage
    }

    private fun findOrCreateDirectChat(
        userId: UserId,
        friendId: UserId
    ): ChatRoom {
        val existingRooms = loadChatRoomPort.findByParticipantId(userId)
        val directRoom = chatRoomDomainService.findDirectChatBetween(
            existingRooms,
            userId,
            friendId
        )

        if (directRoom != null) return directRoom

        val friend = findUserPort.findUserById(friendId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${friendId.value}")

        val newChatRoom = ChatRoom.createDirectChat(
            userId = userId.value,
            friendId = friendId.value,
            friendName = friend.nickname.value
        )

        val savedRoom = saveChatRoomPort.save(newChatRoom)

        chatRoomEventService.createChatRoomCreatedEvents(savedRoom).forEach { event ->
            eventPublisher.publish(event)
        }

        return savedRoom
    }

    private fun forwardMessage(
        originalMessageId: MessageId,
        targetRoomId: ChatRoomId,
        forwardingUserId: UserId
    ): ChatMessage {
        val originalMessage = loadMessagePort.findById(originalMessageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$originalMessageId")

        val forwardedContent = messageForwardDomainService.createForwardedContent(originalMessage)

        val forwardedMessage = messageForwardDomainService.createForwardedMessage(
            targetRoomId = targetRoomId,
            forwardingUserId = forwardingUserId,
            forwardedContent = forwardedContent
        )

        val savedForwardMessage = saveMessagePort.save(forwardedMessage)

        updateChatRoomMetadata(targetRoomId, savedForwardMessage)

        return savedForwardMessage
    }

    private fun updateChatRoomMetadata(
        targetRoomId: ChatRoomId,
        savedForwardMessage: ChatMessage
    ) {
        val chatRoom = loadChatRoomPort.findById(targetRoomId)
            ?: throw ResourceNotFoundException("대상 채팅방을 찾을 수 없습니다. id=$targetRoomId")

        val updatedRoom = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(
            chatRoom = chatRoom,
            message = savedForwardMessage
        )

        saveChatRoomPort.save(updatedRoom)
    }

}