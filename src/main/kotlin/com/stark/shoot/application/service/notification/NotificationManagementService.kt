package com.stark.shoot.application.service.notification

import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.application.port.out.notification.LoadNotificationPort
import com.stark.shoot.application.port.out.notification.SaveNotificationPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.NotificationType
import com.stark.shoot.domain.notification.SourceType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class NotificationManagementService(
    private val loadNotificationPort: LoadNotificationPort,
    private val saveNotificationPort: SaveNotificationPort
) : NotificationManagementUseCase {

    /**
     * 특정 알림을 읽음 처리합니다.
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 객체
     */
    override fun markAsRead(
        notificationId: String,
        userId: Long
    ): Notification {
        val notification = loadNotificationPort.loadNotificationById(notificationId)
            ?: throw IllegalArgumentException("Notification not found with ID: $notificationId")

        // 유저 ID와 알림 ID를 비교하여 알림이 해당 유저의 것인지 확인합니다.
        if (notification.userId != userId) {
            throw IllegalArgumentException("Notification does not belong to user with ID: $userId")
        }

        // 읽음 처리합니다.
        val updatedNotification = notification.markAsRead()

        // 저장합니다.
        return saveNotificationPort.saveNotification(updatedNotification)
    }

    /**
     * 모든 알림을 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsRead(userId: Long): Int {
        val notifications = loadNotificationPort.loadUnreadNotificationsForUser(userId, Int.MAX_VALUE, 0)

        // 모든 읽지 않은 알림을 읽음 처리합니다.
        val updatedNotifications = notifications.map { it.markAsRead() }

        // 저장합니다.
        saveNotificationPort.saveNotifications(updatedNotifications)

        return updatedNotifications.size
    }

    /**
     * 모든 알림을 타입별로 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     * @param type 알림 타입
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsReadByType(
        userId: Long,
        type: NotificationType
    ): Int {
        val notifications = loadNotificationPort.loadNotificationsByType(userId, type, Int.MAX_VALUE, 0)
            .filter { !it.isRead }

        // 모든 읽지 않은 알림을 읽음 처리합니다.
        val updatedNotifications = notifications.map { it.markAsRead() }

        // 저장합니다.
        saveNotificationPort.saveNotifications(updatedNotifications)

        return updatedNotifications.size
    }

    /**
     * 모든 알림을 소스별로 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     * @param sourceType 소스 타입
     * @param sourceId 소스 ID (optional)
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsReadBySource(
        userId: Long,
        sourceType: SourceType,
        sourceId: String?
    ): Int {
        val notifications =
            loadNotificationPort.loadNotificationsBySource(userId, sourceType, sourceId, Int.MAX_VALUE, 0)
                .filter { !it.isRead }

        // 모든 읽지 않은 알림을 읽음 처리합니다.
        val updatedNotifications = notifications.map { it.markAsRead() }

        // 저장합니다.
        saveNotificationPort.saveNotifications(updatedNotifications)

        return updatedNotifications.size
    }

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID
     * @return 삭제 성공 여부
     */
    override fun deleteNotification(
        notificationId: String,
        userId: Long
    ): Boolean {
        val notification = loadNotificationPort.loadNotificationById(notificationId)
            ?: return false

        // 유저 ID와 알림 ID를 비교하여 알림이 해당 유저의 것인지 확인합니다.
        if (notification.userId != userId) {
            return false
        }

        // 읽음 처리된 알림을 삭제합니다. (로직 추가 필요)

        return true
    }

    /**
     * 모든 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @return 삭제된 알림 개수
     */
    override fun deleteAllNotifications(userId: Long): Int {
        // 모든 알림을 삭제합니다. (로직 추가 필요)

        return 0
    }

}