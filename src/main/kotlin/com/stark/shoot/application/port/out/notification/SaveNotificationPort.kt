package com.stark.shoot.application.port.out.notification

import com.stark.shoot.domain.notification.Notification

interface SaveNotificationPort {

    /**
     * 알림을 저장합니다.
     *
     * @param notification 알림 객체
     * @return 저장된 알림 객체
     */
    fun saveNotification(notification: Notification): Notification

    /**
     * 여러 알림을 저장합니다.
     *
     * @param notifications 알림 객체 목록
     * @return 저장된 알림 객체 목록
     */
    fun saveNotifications(notifications: List<Notification>): List<Notification>

}