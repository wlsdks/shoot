package com.stark.shoot.application.port.out.notification

import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationId

interface LoadNotificationPort {

    fun loadNotificationById(id: NotificationId): Notification?
    fun loadNotificationsForUser(userId: UserId, limit: Int = 20, offset: Int = 0): List<Notification>
    fun loadUnreadNotificationsForUser(userId: UserId, limit: Int = 20, offset: Int = 0): List<Notification>
    fun loadNotificationsByType(userId: UserId, type: NotificationType, limit: Int = 20, offset: Int = 0): List<Notification>
    fun loadNotificationsBySource(
        userId: UserId,
        sourceType: SourceType, 
        sourceId: String? = null, 
        limit: Int = 20, 
        offset: Int = 0
    ): List<Notification>
    fun countUnreadNotifications(userId: UserId): Int

}