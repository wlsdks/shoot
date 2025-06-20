package com.stark.shoot.application.port.`in`.notification

import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationId
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType

interface NotificationManagementUseCase {

    fun markAsRead(notificationId: NotificationId, userId: UserId): Notification
    fun markAllAsRead(userId: UserId): Int
    fun markAllAsReadByType(userId: UserId, type: NotificationType): Int
    fun markAllAsReadBySource(userId: UserId, sourceType: SourceType, sourceId: String? = null): Int
    fun deleteNotification(notificationId: NotificationId, userId: UserId): Boolean
    fun deleteAllNotifications(userId: UserId): Int

}