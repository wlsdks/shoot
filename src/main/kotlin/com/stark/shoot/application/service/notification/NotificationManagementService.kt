package com.stark.shoot.application.service.notification

import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.application.port.out.notification.NotificationCommandPort
import com.stark.shoot.application.port.out.notification.NotificationQueryPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.service.NotificationDomainService
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.exception.NotificationException
import com.stark.shoot.infrastructure.exception.web.MongoOperationException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 알림 관리 서비스
 *
 * 알림 관리와 관련된 유스케이스를 구현하는 애플리케이션 서비스입니다.
 * 도메인 로직은 도메인 모델과 도메인 서비스에 위임하고,
 * 이 서비스는 주로 유스케이스 흐름 조정과 인프라스트럭처 계층과의 통합을 담당합니다.
 */
@Transactional
@Service
class NotificationManagementService(
    private val notificationQueryPort: NotificationQueryPort,
    private val notificationCommandPort: NotificationCommandPort,
    private val notificationDomainService: NotificationDomainService
) : NotificationManagementUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 특정 알림을 읽음 처리합니다.
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 객체
     * @throws ResourceNotFoundException 알림이 존재하지 않는 경우
     * @throws NotificationException 알림이 해당 유저의 것이 아닌 경우
     */
    override fun markAsRead(
        notificationId: NotificationId,
        userId: UserId
    ): Notification {
        // 알림 조회
        val notification = notificationQueryPort.loadNotificationById(notificationId)
            ?: throw ResourceNotFoundException("알림을 찾을 수 없습니다: $notificationId")

        // 도메인 모델의 메서드를 사용하여 소유권 검증
        notification.validateOwnership(userId)

        // 이미 읽은 알림인 경우 바로 반환
        if (notification.isRead) {
            return notification
        }

        // 도메인 모델의 메서드를 사용하여 읽음 처리
        val updatedNotification = notification.markAsRead()

        // 저장
        val savedNotification = notificationCommandPort.saveNotification(updatedNotification)

        return savedNotification
    }

    /**
     * 모든 알림을 읽음 처리합니다.
     *
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsRead(userId: UserId): Int {
        // 읽지 않은 알림 조회
        val notifications = notificationQueryPort.loadUnreadNotificationsForUser(userId, Int.MAX_VALUE, 0)

        if (notifications.isEmpty()) {
            return 0
        }

        // 도메인 서비스를 사용하여 모든 알림 읽음 처리
        val updatedNotifications = notificationDomainService.markNotificationsAsRead(notifications)

        // 저장
        notificationCommandPort.saveNotifications(updatedNotifications)

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
        userId: UserId,
        type: NotificationType
    ): Int {
        // 특정 타입의 알림 조회
        val notifications = notificationQueryPort.loadNotificationsByType(userId, type, Int.MAX_VALUE, 0)

        // 읽지 않은 알림만 필터링
        val unreadNotifications = notificationDomainService.filterUnread(notifications)

        if (unreadNotifications.isEmpty()) {
            return 0
        }

        // 도메인 서비스를 사용하여 알림 읽음 처리
        val updatedNotifications = notificationDomainService.markNotificationsAsRead(unreadNotifications)

        // 저장
        notificationCommandPort.saveNotifications(updatedNotifications)

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
        userId: UserId,
        sourceType: SourceType,
        sourceId: String?
    ): Int {
        // 특정 소스의 알림 조회
        val notifications = notificationQueryPort.loadNotificationsBySource(
            userId, sourceType, sourceId, Int.MAX_VALUE, 0
        )

        // 읽지 않은 알림만 필터링
        val unreadNotifications = notificationDomainService.filterUnread(notifications)

        if (unreadNotifications.isEmpty()) {
            return 0
        }

        // 도메인 서비스를 사용하여 알림 읽음 처리
        val updatedNotifications = notificationDomainService.markNotificationsAsRead(unreadNotifications)

        // 저장
        notificationCommandPort.saveNotifications(updatedNotifications)

        return updatedNotifications.size
    }

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID
     * @return 삭제 성공 여부
     * @throws ResourceNotFoundException 알림을 찾을 수 없는 경우
     * @throws NotificationException 알림이 해당 사용자의 것이 아닌 경우
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteNotification(
        notificationId: NotificationId,
        userId: UserId
    ): Boolean {
        // 알림 조회
        val notification = notificationQueryPort.loadNotificationById(notificationId)
            ?: throw ResourceNotFoundException("알림을 찾을 수 없습니다: $notificationId")

        // 도메인 모델의 메서드를 사용하여 소유권 검증
        notification.validateOwnership(userId)

        // 알림 삭제 (소프트 삭제 방식)
        val deletedNotification = notification.markAsDeleted()
        notificationCommandPort.saveNotification(deletedNotification)

        return true
    }

    /**
     * 모든 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @return 삭제된 알림 개수
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteAllNotifications(userId: UserId): Int {
        // 사용자의 모든 알림 조회
        val notifications = notificationQueryPort.loadNotificationsForUser(userId, Int.MAX_VALUE, 0)

        if (notifications.isEmpty()) {
            return 0
        }

        // 도메인 서비스를 사용하여 모든 알림 삭제 처리
        val deletedNotifications = notificationDomainService.markNotificationsAsDeleted(notifications)

        // 저장
        notificationCommandPort.saveNotifications(deletedNotifications)

        return deletedNotifications.size
    }

}
