package com.stark.shoot.application.port.`in`.notification

import com.stark.shoot.application.port.`in`.notification.command.*
import com.stark.shoot.domain.notification.Notification

interface NotificationQueryUseCase {

    fun getNotificationsForUser(command: GetNotificationsCommand): List<Notification>
    fun getUnreadNotificationsForUser(command: GetUnreadNotificationsCommand): List<Notification>
    fun getNotificationsByType(command: GetNotificationsByTypeCommand): List<Notification>
    fun getNotificationsBySource(command: GetNotificationsBySourceCommand): List<Notification>
    fun getUnreadNotificationCount(command: GetUnreadNotificationCountCommand): Int

}
