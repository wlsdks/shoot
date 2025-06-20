package com.stark.shoot.application.port.out.notification

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType
import com.stark.shoot.domain.notification.NotificationId

interface LoadNotificationPort {

    fun loadNotificationById(id: NotificationId): Notification?
    fun loadNotificationsForUser(userId: Long, limit: Int = 20, offset: Int = 0): List<Notification>
    fun loadUnreadNotificationsForUser(userId: Long, limit: Int = 20, offset: Int = 0): List<Notification>
    fun loadNotificationsByType(userId: Long, type: NotificationType, limit: Int = 20, offset: Int = 0): List<Notification>
    fun loadNotificationsBySource(
        userId: Long, 
        sourceType: SourceType, 
        sourceId: String? = null, 
        limit: Int = 20, 
        offset: Int = 0
    ): List<Notification>
    fun countUnreadNotifications(userId: Long): Int

}