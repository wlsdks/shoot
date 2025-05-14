package com.stark.shoot.application.port.out.notification

import com.stark.shoot.domain.notification.Notification

interface SendNotificationPort {

    fun sendNotification(notification: Notification): Boolean
    fun sendNotifications(notifications: List<Notification>): Int

}