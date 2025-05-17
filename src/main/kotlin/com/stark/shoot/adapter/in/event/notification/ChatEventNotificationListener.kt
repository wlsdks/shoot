package com.stark.shoot.adapter.`in`.event.notification

import com.stark.shoot.application.port.out.notification.SaveNotificationPort
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ChatEventNotificationListener(
    private val saveNotificationPort: SaveNotificationPort,
    private val sendNotificationPort: SendNotificationPort
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 알림을 처리하는 메서드입니다.
     *
     * @param event ChatEvent
     */
    @EventListener
    fun handleChatEvent(event: ChatEvent) {
        handleMessageCreated(event)
    }

    /**
     * 생성된 메시지에 대한 알림을 처리합니다.
     * 비즈니스 로직을 직접 구현하여 알림을 생성하고 저장 및 전송합니다.
     *
     * @param event ChatEvent
     */
    private fun handleMessageCreated(event: ChatEvent) {
        val message = event.data

        try {
            // 자신의 메시지에 대해 알림이 전송되지 않도록 합니다.
            val recipients = message.readBy.keys.filter { it != message.senderId }.toSet()

            if (recipients.isEmpty()) {
                logger.info { "알림 수신자가 없습니다: roomId=${message.roomId}, messageId=${message.id}" }
                return
            }

            val notifications = mutableListOf<Notification>()

            // 메시지에 멘션된 사용자가 있는 경우
            if (message.mentions.isNotEmpty()) {
                // 멘션된 사용자에 대한 알림 생성
                val mentionedUsers = message.mentions.filter { it != message.senderId }.toSet()

                if (mentionedUsers.isNotEmpty()) {
                    // 멘션된 사용자에 대한 알림 생성
                    val mentionNotifications = mentionedUsers.map { userId ->
                        Notification.fromChatEvent(
                            userId = userId,
                            title = "새로운 멘션",
                            message = "메시지에서 언급되었습니다: ${message.content.text.take(50)}${if (message.content.text.length > 50) "..." else ""}",
                            type = NotificationType.MENTION,
                            sourceId = message.roomId.toString(),
                            metadata = mapOf(
                                "senderId" to message.senderId.toString(),
                                "messageId" to message.id.toString()
                            )
                        )
                    }
                    notifications.addAll(mentionNotifications)
                }
            }

            // 모든 수신자에 대한 일반 알림 생성
            val messageNotifications = recipients.map { userId ->
                Notification.fromChatEvent(
                    userId = userId,
                    title = "새로운 메시지",
                    message = message.content.text.take(50) + if (message.content.text.length > 50) "..." else "",
                    type = NotificationType.NEW_MESSAGE,
                    sourceId = message.roomId.toString(),
                    metadata = mapOf(
                        "senderId" to message.senderId.toString(),
                        "messageId" to message.id.toString()
                    )
                )
            }
            notifications.addAll(messageNotifications)

            // DB에 알림 저장
            val savedNotifications = saveNotificationPort.saveNotifications(notifications)

            // 실시간 알림 전송
            sendNotificationPort.sendNotifications(savedNotifications)

            logger.info { "채팅 이벤트에서 ${savedNotifications.size}개의 알림이 생성되고 전송되었습니다: roomId=${message.roomId}" }
        } catch (e: Exception) {
            logger.error(e) { "채팅 이벤트 처리 중 오류가 발생했습니다: ${e.message}" }
        }
    }

}
