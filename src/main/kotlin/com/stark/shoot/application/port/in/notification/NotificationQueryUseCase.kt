package com.stark.shoot.application.port.`in`.notification

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType

interface NotificationQueryUseCase {

    fun getNotificationsForUser(userId: Long, limit: Int = 20, offset: Int = 0): List<Notification>
    fun getUnreadNotificationsForUser(userId: Long, limit: Int = 20, offset: Int = 0): List<Notification>
    fun getNotificationsByType(userId: Long, type: NotificationType, limit: Int = 20, offset: Int = 0): List<Notification>
    fun getNotificationsBySource(
        userId: Long, 
        sourceType: SourceType, 
        sourceId: String? = null, 
        limit: Int = 20, 
        offset: Int = 0
    ): List<Notification>
    fun getUnreadNotificationCount(userId: Long): Int

}