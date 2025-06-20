package com.stark.shoot.domain.notification.service

import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.event.NotificationEvent

/**
 * 알림 도메인 서비스
 *
 * 알림 도메인 모델에 대한 비즈니스 로직을 처리하는 서비스입니다.
 * 단일 알림 엔티티로 처리할 수 없는 복잡한 도메인 로직을 담당합니다.
 */
class NotificationDomainService {

    /**
     * 여러 알림을 읽음 처리합니다.
     *
     * @param notifications 알림 목록
     * @return 읽음 처리된 알림 목록
     */
    fun markNotificationsAsRead(
        notifications: List<Notification>
    ): List<Notification> {
        return notifications.map { it.markAsRead() }
    }

    /**
     * 여러 알림을 삭제 처리합니다.
     *
     * @param notifications 알림 목록
     * @return 삭제 처리된 알림 목록
     */
    fun markNotificationsAsDeleted(
        notifications: List<Notification>
    ): List<Notification> {
        return notifications.map { it.markAsDeleted() }
    }

    /**
     * 읽지 않은 알림만 필터링합니다.
     *
     * @param notifications 알림 목록
     * @return 필터링된 알림 목록
     */
    fun filterUnread(
        notifications: List<Notification>
    ): List<Notification> {
        return notifications.filter { !it.isRead }
    }

    /**
     * 도메인 이벤트로부터 알림 목록을 생성합니다.
     *
     * @param event 알림 이벤트
     * @return 수신자별로 생성된 알림 목록
     */
    fun createNotificationsFromEvent(event: NotificationEvent): List<Notification> {
        val recipients = event.getRecipients()
        if (recipients.isEmpty()) return emptyList()
        return recipients.map { recipientId ->
            Notification.fromEvent(event, recipientId)
        }
    }

}
