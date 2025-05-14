package com.stark.shoot.application.port.out.notification

import com.stark.shoot.domain.notification.Notification

interface SaveNotificationPort {

    fun saveNotification(notification: Notification): Notification
    fun saveNotifications(notifications: List<Notification>): List<Notification>

}