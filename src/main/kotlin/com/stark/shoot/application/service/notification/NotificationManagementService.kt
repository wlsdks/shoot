package com.stark.shoot.application.service.notification

import com.stark.shoot.application.port.`in`.notification.NotificationManagementUseCase
import com.stark.shoot.application.port.`in`.notification.command.*
import com.stark.shoot.application.port.out.notification.NotificationCommandPort
import com.stark.shoot.application.port.out.notification.NotificationQueryPort
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.service.NotificationDomainService
import com.stark.shoot.domain.exception.NotificationException
import com.stark.shoot.domain.exception.web.MongoOperationException
import com.stark.shoot.domain.exception.web.ResourceNotFoundException
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
     * @param command 알림 읽음 처리 커맨드
     * @return 읽음 처리된 알림 객체
     * @throws ResourceNotFoundException 알림이 존재하지 않는 경우
     * @throws NotificationException 알림이 해당 유저의 것이 아닌 경우
     */
    override fun markAsRead(command: MarkNotificationAsReadCommand): Notification {
        // 알림 조회
        val notification = notificationQueryPort.loadNotificationById(command.notificationId)
            ?: throw ResourceNotFoundException("알림을 찾을 수 없습니다: ${command.notificationId}")

        // 도메인 모델의 메서드를 사용하여 소유권 검증
        notification.validateOwnership(command.userId)

        // 이미 읽은 알림인 경우 바로 반환
        if (notification.isRead) {
            return notification
        }

        // 도메인 모델의 메서드를 사용하여 읽음 처리 (자신의 상태 변경)
        notification.markAsRead()

        // 저장
        val savedNotification = notificationCommandPort.saveNotification(notification)

        return savedNotification
    }

    /**
     * 알림 목록을 읽음 처리하는 공통 로직을 수행하는 내부 메서드
     *
     * @param notifications 읽음 처리할 알림 목록
     * @param filterUnread 읽지 않은 알림만 필터링할지 여부
     * @return 읽음 처리된 알림 개수
     */
    private fun processMarkAsRead(notifications: List<Notification>, filterUnread: Boolean = true): Int {
        // 알림이 없으면 0 반환
        if (notifications.isEmpty()) {
            return 0
        }

        // 필요한 경우 읽지 않은 알림만 필터링
        val notificationsToMark = if (filterUnread) {
            notificationDomainService.filterUnread(notifications)
        } else {
            notifications
        }

        // 필터링 후 알림이 없으면 0 반환
        if (notificationsToMark.isEmpty()) {
            return 0
        }

        // 도메인 서비스를 사용하여 알림 읽음 처리
        notificationDomainService.markNotificationsAsRead(notificationsToMark)

        // 저장
        notificationCommandPort.saveNotifications(notificationsToMark)

        return notificationsToMark.size
    }

    /**
     * 모든 알림을 읽음 처리합니다.
     *
     * @param command 모든 알림 읽음 처리 커맨드
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsRead(command: MarkAllNotificationsAsReadCommand): Int {
        // 읽지 않은 알림 조회
        val notifications = notificationQueryPort.loadUnreadNotificationsForUser(command.userId, Int.MAX_VALUE, 0)
        
        // 이미 필터링된 알림이므로 추가 필터링 불필요
        return processMarkAsRead(notifications, filterUnread = false)
    }

    /**
     * 모든 알림을 타입별로 읽음 처리합니다.
     *
     * @param command 타입별 알림 읽음 처리 커맨드
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsReadByType(command: MarkAllNotificationsByTypeAsReadCommand): Int {
        // 특정 타입의 알림 조회
        val notifications = notificationQueryPort.loadNotificationsByType(command.userId, command.type, Int.MAX_VALUE, 0)
        
        // 읽지 않은 알림만 처리
        return processMarkAsRead(notifications)
    }

    /**
     * 모든 알림을 소스별로 읽음 처리합니다.
     *
     * @param command 소스별 알림 읽음 처리 커맨드
     * @return 읽음 처리된 알림 개수
     */
    override fun markAllAsReadBySource(command: MarkAllNotificationsBySourceAsReadCommand): Int {
        // 특정 소스의 알림 조회
        val notifications = notificationQueryPort.loadNotificationsBySource(
            command.userId, command.sourceType, command.sourceId, Int.MAX_VALUE, 0
        )
        
        // 읽지 않은 알림만 처리
        return processMarkAsRead(notifications)
    }

    /**
     * 알림 목록을 삭제 처리하는 공통 로직을 수행하는 내부 메서드
     *
     * @param notifications 삭제 처리할 알림 목록
     * @return 삭제 처리된 알림 개수
     */
    private fun processDeleteNotifications(notifications: List<Notification>): Int {
        if (notifications.isEmpty()) {
            return 0
        }

        // 도메인 서비스를 사용하여 알림 삭제 처리
        notificationDomainService.markNotificationsAsDeleted(notifications)

        // 저장
        notificationCommandPort.saveNotifications(notifications)

        return notifications.size
    }

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param command 알림 삭제 커맨드
     * @return 삭제 성공 여부
     * @throws ResourceNotFoundException 알림을 찾을 수 없는 경우
     * @throws NotificationException 알림이 해당 사용자의 것이 아닌 경우
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteNotification(command: DeleteNotificationCommand): Boolean {
        // 알림 조회
        val notification = notificationQueryPort.loadNotificationById(command.notificationId)
            ?: throw ResourceNotFoundException("알림을 찾을 수 없습니다: ${command.notificationId}")

        // 도메인 모델의 메서드를 사용하여 소유권 검증
        notification.validateOwnership(command.userId)

        // 알림 삭제 (소프트 삭제 방식)
        notification.markAsDeleted()
        notificationCommandPort.saveNotification(notification)

        return true
    }

    /**
     * 모든 알림을 삭제합니다.
     *
     * @param command 모든 알림 삭제 커맨드
     * @return 삭제된 알림 개수
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    override fun deleteAllNotifications(command: DeleteAllNotificationsCommand): Int {
        // 사용자의 모든 알림 조회
        val notifications = notificationQueryPort.loadNotificationsForUser(command.userId, Int.MAX_VALUE, 0)
        
        // 공통 로직을 사용하여 알림 삭제 처리
        return processDeleteNotifications(notifications)
    }

}
