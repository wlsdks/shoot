package com.stark.shoot.application.service.notification

import com.stark.shoot.application.port.`in`.notification.SendNotificationUseCase
import com.stark.shoot.application.port.out.notification.PublishNotificationEventPort
import com.stark.shoot.application.port.out.notification.SaveNotificationPort
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.event.NotificationEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class SendNotificationService(
    private val saveNotificationPort: SaveNotificationPort,
    private val sendNotificationPort: SendNotificationPort,
    private val publishNotificationEventPort: PublishNotificationEventPort
) : SendNotificationUseCase {


    /**
     * 메시지 알림 전송
     *
     * @param notification 알림 객체
     * @return 전송된 알림 객체
     */
    override fun sendNotification(
        notification: Notification
    ): Notification {
        // DB에 알림 저장
        val savedNotification = saveNotificationPort.saveNotification(notification)

        // 사용자에게 알림 전송
        sendNotificationPort.sendNotification(savedNotification)

        return savedNotification
    }


    /**
     * 알림 이벤트 처리
     *
     * @param event 알림 이벤트 객체
     * @return 전송된 알림 목록
     */
    override fun processNotificationEvent(
        event: NotificationEvent
    ): List<Notification> {
        // 이벤트 발행
        publishNotificationEventPort.publishEvent(event)

        // 알림 수신자 목록을 기반으로 알림 객체 생성
        val notifications = event.getRecipients().map { recipientId ->
            Notification(
                userId = recipientId,
                title = event.getTitle(),
                message = event.getMessage(),
                type = event.type,
                sourceId = event.sourceId,
                sourceType = event.sourceType,
                metadata = event.metadata
            )
        }

        // DB에 알림 저장
        val savedNotifications = saveNotificationPort.saveNotifications(notifications)

        // 사용자에게 알림 전송
        sendNotificationPort.sendNotifications(savedNotifications)

        return savedNotifications
    }

}