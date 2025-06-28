package com.stark.shoot.application.port.`in`.notification

import com.stark.shoot.application.port.`in`.notification.command.*
import com.stark.shoot.domain.notification.Notification

interface NotificationManagementUseCase {

    fun markAsRead(command: MarkNotificationAsReadCommand): Notification
    fun markAllAsRead(command: MarkAllNotificationsAsReadCommand): Int
    fun markAllAsReadByType(command: MarkAllNotificationsByTypeAsReadCommand): Int
    fun markAllAsReadBySource(command: MarkAllNotificationsBySourceAsReadCommand): Int
    fun deleteNotification(command: DeleteNotificationCommand): Boolean
    fun deleteAllNotifications(command: DeleteAllNotificationsCommand): Int

}
