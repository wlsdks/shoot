package com.stark.shoot.application.port.`in`.notification

import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType

interface NotificationQueryUseCase {

    fun getNotificationsForUser(userId: UserId, limit: Int = 20, offset: Int = 0): List<Notification>
    fun getUnreadNotificationsForUser(userId: UserId, limit: Int = 20, offset: Int = 0): List<Notification>
    fun getNotificationsByType(userId: UserId, type: NotificationType, limit: Int = 20, offset: Int = 0): List<Notification>
    fun getNotificationsBySource(
        userId: UserId,
        sourceType: SourceType, 
        sourceId: String? = null, 
        limit: Int = 20, 
        offset: Int = 0
    ): List<Notification>
    fun getUnreadNotificationCount(userId: UserId): Int

}