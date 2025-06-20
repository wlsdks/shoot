package com.stark.shoot.adapter.`in`.event.notification

import com.stark.shoot.application.port.out.notification.SaveNotificationPort
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.notification.Notification
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 채팅 이벤트를 수신하여 알림을 생성하고 전송하는 리스너 클래스입니다.
 *
 * 이 클래스는 채팅 메시지 생성 이벤트를 수신하여 다음과 같은 작업을 수행합니다:
 * 1. 메시지 수신자 식별
 * 2. 멘션된 사용자에 대한 알림 생성
 * 3. 일반 메시지 수신자에 대한 알림 생성
 * 4. 알림 저장 및 전송
 */
@Component
class ChatEventNotificationListener(
    private val saveNotificationPort: SaveNotificationPort,
    private val sendNotificationPort: SendNotificationPort,
    private val chatNotificationFactory: ChatNotificationFactory
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 채팅 이벤트를 수신하여 알림 처리를 시작하는 메서드입니다.
     *
     * @param event 처리할 채팅 이벤트
     */
    @EventListener
    fun handleChatEvent(event: ChatEvent) {
        handleMessageCreated(event)
    }

    /**
     * 생성된 메시지에 대한 알림을 처리합니다.
     * 메시지 수신자를 식별하고, 알림을 생성하여 저장 및 전송합니다.
     *
     * @param event 처리할 채팅 이벤트
     */
    private fun handleMessageCreated(event: ChatEvent) {
        val message = event.data

        try {
            // 메시지 수신자 식별
            val recipients = identifyRecipients(message)
            if (recipients.isEmpty()) {
                logger.info { "알림 수신자가 없습니다: roomId=${message.roomId}, messageId=${message.id}" }
                return
            }

            // 알림 생성
            val notifications = createNotifications(message, recipients)

            // 알림 저장 및 전송
            processNotifications(notifications, message.roomId.value)

        } catch (e: Exception) {
            logger.error(e) { "채팅 이벤트 처리 중 오류가 발생했습니다: ${e.message}" }
        }
    }

    /**
     * 메시지 수신자를 식별합니다.
     * 자신의 메시지에 대해서는 알림이 전송되지 않도록 합니다.
     *
     * @param message 채팅 메시지
     * @return 알림을 받을 사용자 ID 집합
     */
    private fun identifyRecipients(message: ChatMessage): Set<Long> {
        return message.readBy.keys.filter { it != message.senderId }.map { it.value }.toSet()
    }

    /**
     * 메시지에 대한 알림을 생성합니다.
     * 멘션된 사용자에 대한 알림과 일반 메시지 수신자에 대한 알림을 생성합니다.
     *
     * @param message 채팅 메시지
     * @param recipients 알림을 받을 사용자 ID 집합
     * @return 생성된 알림 목록
     */
    private fun createNotifications(
        message: ChatMessage,
        recipients: Set<Long>
    ): List<Notification> {
        val notifications = mutableListOf<Notification>()

        // 멘션 알림 생성
        val mentionNotifications = createMentionNotifications(message)
        notifications.addAll(mentionNotifications)

        // 일반 메시지 알림 생성
        val messageNotifications = createMessageNotifications(message, recipients)
        notifications.addAll(messageNotifications)

        return notifications
    }

    /**
     * 멘션된 사용자에 대한 알림을 생성합니다.
     *
     * @param message 채팅 메시지
     * @return 생성된 멘션 알림 목록
     */
    private fun createMentionNotifications(
        message: ChatMessage
    ): List<Notification> {
        // 멘션된 사용자가 없으면 빈 리스트 반환
        if (message.mentions.isEmpty()) {
            return emptyList()
        }

        // 자신을 멘션한 경우는 제외
        val mentionedUsers = message.mentions.filter { it != message.senderId }.toSet()
        if (mentionedUsers.isEmpty()) {
            return emptyList()
        }

        // 멘션된 사용자에 대한 알림 생성
        return mentionedUsers.map { userId ->
            chatNotificationFactory.createMentionNotification(
                userId = userId.value,
                message = message
            )
        }
    }

    /**
     * 일반 메시지 수신자에 대한 알림을 생성합니다.
     *
     * @param message 채팅 메시지
     * @param recipients 알림을 받을 사용자 ID 집합
     * @return 생성된 메시지 알림 목록
     */
    private fun createMessageNotifications(
        message: ChatMessage,
        recipients: Set<Long>
    ): List<Notification> {
        return recipients.map { userId ->
            chatNotificationFactory.createMessageNotification(
                userId = userId,
                message = message
            )
        }
    }

    /**
     * 생성된 알림을 저장하고 전송합니다.
     *
     * @param notifications 저장 및 전송할 알림 목록
     * @param roomId 채팅방 ID (로깅용)
     */
    private fun processNotifications(
        notifications: List<Notification>,
        roomId: Long
    ) {
        if (notifications.isEmpty()) {
            logger.info { "생성된 알림이 없습니다: roomId=$roomId" }
            return
        }

        // DB에 알림 저장
        val savedNotifications = saveNotificationPort.saveNotifications(notifications)

        // 실시간 알림 전송
        sendNotificationPort.sendNotifications(savedNotifications)

        logger.info { "채팅 이벤트에서 ${savedNotifications.size}개의 알림이 생성되고 전송되었습니다: roomId=$roomId" }
    }

}
