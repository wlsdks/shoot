package com.stark.shoot.application.port.out.notification

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.exception.web.RedisOperationException

interface SendNotificationPort {

    /**
     * 알림을 전송합니다.
     *
     * @param notification 알림 객체
     * @throws RedisOperationException 알림 전송 실패 시 발생
     */
    fun sendNotification(notification: Notification)

    /**
     * 여러 알림을 전송합니다.
     *
     * @param notifications 알림 객체 목록
     * @throws RedisOperationException 알림 전송 실패 시 발생
     */
    fun sendNotifications(notifications: List<Notification>)

}
