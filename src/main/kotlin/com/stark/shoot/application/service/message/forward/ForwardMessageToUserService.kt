package com.stark.shoot.application.service.message.forward

import com.stark.shoot.application.port.`in`.message.ForwardMessageToUserUseCase
import com.stark.shoot.application.port.`in`.message.command.ForwardMessageToUserCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageForwardDomainService
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.domain.chatroom.service.ChatRoomEventService
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class ForwardMessageToUserService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort,
    private val userQueryPort: UserQueryPort,
    private val eventPublisher: EventPublishPort,
    private val chatRoomEventService: ChatRoomEventService,
    private val chatRoomDomainService: ChatRoomDomainService,
    private val messageForwardDomainService: MessageForwardDomainService,
    private val chatRoomMetadataDomainService: ChatRoomMetadataDomainService
) : ForwardMessageToUserUseCase {

    /**
     * 메시지를 특정 사용자(친구)에게 전달합니다.
     * 1:1 채팅방을 생성하거나 조회한 후, 해당 채팅방으로 메시지를 전달합니다.
     *
     * @param command 메시지 전달 커맨드 (원본 메시지 ID, 대상 사용자 ID, 전달하는 사용자 ID)
     * @return 전달된 메시지
     */
    override fun forwardMessageToUser(command: ForwardMessageToUserCommand): ChatMessage {
        // 1. 사용자 간 1:1 채팅방 조회 또는 생성
        val chatRoom = findOrCreateDirectChat(command.forwardingUserId, command.targetUserId)

        val roomId = chatRoom.id?.value
            ?: throw IllegalStateException("채팅방 ID가 null입니다.")

        // 2. 해당 채팅방으로 메시지 전달
        val forwardedMessage = forwardMessage(
            originalMessageId = command.originalMessageId,
            targetRoomId = ChatRoomId.from(roomId),
            forwardingUserId = command.forwardingUserId
        )

        return forwardedMessage
    }

    private fun findOrCreateDirectChat(
        userId: UserId,
        friendId: UserId
    ): ChatRoom {
        val existingRooms = chatRoomQueryPort.findByParticipantId(userId)
        val directRoom = chatRoomDomainService.findDirectChatBetween(
            existingRooms,
            userId,
            friendId
        )

        if (directRoom != null) return directRoom

        val friend = userQueryPort.findUserById(friendId)
            ?: throw ResourceNotFoundException("사용자를 찾을 수 없습니다: ${friendId.value}")

        val newChatRoom = ChatRoom.createDirectChat(
            userId = userId.value,
            friendId = friendId.value,
            friendName = friend.nickname.value
        )

        val savedRoom = chatRoomCommandPort.save(newChatRoom)

        chatRoomEventService.createChatRoomCreatedEvents(savedRoom).forEach { event ->
            eventPublisher.publishEvent(event)
        }

        return savedRoom
    }

    private fun forwardMessage(
        originalMessageId: MessageId,
        targetRoomId: ChatRoomId,
        forwardingUserId: UserId
    ): ChatMessage {
        val originalMessage = messageQueryPort.findById(originalMessageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$originalMessageId")

        val forwardedContent = messageForwardDomainService.createForwardedContent(originalMessage)

        val forwardedMessage = messageForwardDomainService.createForwardedMessage(
            targetRoomId = targetRoomId,
            forwardingUserId = forwardingUserId,
            forwardedContent = forwardedContent
        )

        val savedForwardMessage = messageCommandPort.save(forwardedMessage)

        updateChatRoomMetadata(targetRoomId, savedForwardMessage)

        return savedForwardMessage
    }

    private fun updateChatRoomMetadata(
        targetRoomId: ChatRoomId,
        savedForwardMessage: ChatMessage
    ) {
        val chatRoom = chatRoomQueryPort.findById(targetRoomId)
            ?: throw ResourceNotFoundException("대상 채팅방을 찾을 수 없습니다. id=$targetRoomId")

        val updatedRoom = chatRoomMetadataDomainService.updateChatRoomWithNewMessage(
            chatRoom = chatRoom,
            message = savedForwardMessage
        )

        chatRoomCommandPort.save(updatedRoom)
    }

}
