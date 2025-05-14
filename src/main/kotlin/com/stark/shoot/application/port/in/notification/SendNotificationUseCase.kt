package com.stark.shoot.application.port.`in`.notification

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.event.NotificationEvent

interface SendNotificationUseCase {

    fun sendNotification(notification: Notification): Notification
    fun processNotificationEvent(event: NotificationEvent): List<Notification>

}