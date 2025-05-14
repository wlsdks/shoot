package com.stark.shoot.adapter.`in`.event.notification

import com.stark.shoot.application.port.`in`.notification.SendNotificationUseCase
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.notification.event.NewMessageEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ChatEventNotificationListener(
    private val sendNotificationUseCase: SendNotificationUseCase
) {

    /**
     * 알림을 처리하는 메서드입니다.
     *
     * @param event ChatEvent
     */
    @EventListener
    fun handleChatEvent(event: ChatEvent) {
        when (event.type) {
            EventType.MESSAGE_CREATED -> handleMessageCreated(event)
            EventType.MESSAGE_UPDATED -> handleMessageUpdated(event)
            EventType.MESSAGE_DELETED -> handleMessageDeleted(event)
        }
    }

    /**
     * 생성된 메시지에 대한 알림을 처리합니다.
     *
     * @param event ChatEvent
     */
    private fun handleMessageCreated(event: ChatEvent) {
        val message = event.data

        // 자신의 메시지에 대해 알림이 전송되지 않도록 합니다.
        val recipients = message.readBy.keys.filter { it != message.senderId }.toSet()

        if (recipients.isNotEmpty()) {
            // 메시지에 멘션된 사용자가 있는 경우
            if (message.mentions.isNotEmpty()) {
                // 멘션된 사용자에 대한 알림 생성
                val mentionedUsers = message.mentions.filter { it != message.senderId }.toSet()

                if (mentionedUsers.isNotEmpty()) {
                    val notificationEvent = NewMessageEvent(
                        roomId = message.roomId,
                        senderId = message.senderId,
                        senderName = "User", // We don't have the sender name in the message
                        messageContent = "Mentioned you in a message: ${message.content.text.take(50)}${if (message.content.text.length > 50) "..." else ""}",
                        recipients = mentionedUsers
                    )

                    sendNotificationUseCase.processNotificationEvent(notificationEvent)
                }
            }

            // 모든 수신자에 대한 일반 알림 생성
            val notificationEvent = NewMessageEvent(
                roomId = message.roomId,
                senderId = message.senderId,
                senderName = "User", // 지금은 메시지에서 발신자 이름을 알 수 없습니다.
                messageContent = message.content.text.take(50) + if (message.content.text.length > 50) "..." else "",
                recipients = recipients
            )

            sendNotificationUseCase.processNotificationEvent(notificationEvent)
        }
    }

    /**
     * 메시지 업데이트에 대한 알림을 처리합니다.
     *
     * @param event ChatEvent
     */
    private fun handleMessageUpdated(event: ChatEvent) {
        // 추후 로직 추가 예정
    }

    /**
     * 메시지 삭제에 대한 알림을 처리합니다.
     *
     * @param event ChatEvent
     */
    private fun handleMessageDeleted(event: ChatEvent) {
        // 추후 로직 추가 예정
    }

}
