package com.stark.shoot.application.port.`in`.notification

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType

interface NotificationManagementUseCase {

    fun markAsRead(notificationId: String, userId: Long): Notification
    fun markAllAsRead(userId: Long): Int
    fun markAllAsReadByType(userId: Long, type: NotificationType): Int
    fun markAllAsReadBySource(userId: Long, sourceType: SourceType, sourceId: String? = null): Int
    fun deleteNotification(notificationId: String, userId: Long): Boolean
    fun deleteAllNotifications(userId: Long): Int

}