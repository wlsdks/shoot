package com.stark.shoot.application.service.notification

import com.stark.shoot.application.port.`in`.notification.NotificationQueryUseCase
import com.stark.shoot.application.port.`in`.notification.command.*
import com.stark.shoot.application.port.out.notification.NotificationQueryPort
import com.stark.shoot.domain.notification.Notification
import org.springframework.stereotype.Service

@Service
class NotificationQueryService(
    private val notificationQueryPort: NotificationQueryPort
) : NotificationQueryUseCase {

    /**
     * 유저에 대한 알림을 가져옵니다.
     * 기본적으로 최신 20개의 알림을 조회합니다. (limit=20, offset=0)
     *
     * @param command 알림 조회 커맨드
     * @return 알림 목록
     */
    override fun getNotificationsForUser(command: GetNotificationsCommand): List<Notification> {
        return notificationQueryPort.loadNotificationsForUser(command.userId, command.limit, command.offset)
    }

    /**
     * 유저에 대한 읽지 않은 알림을 가져옵니다.
     * 기본적으로 최신 20개의 읽지 않은 알림을 조회합니다. (limit=20, offset=0)
     *
     * @param command 읽지 않은 알림 조회 커맨드
     * @return 읽지 않은 알림 목록
     */
    override fun getUnreadNotificationsForUser(command: GetUnreadNotificationsCommand): List<Notification> {
        return notificationQueryPort.loadUnreadNotificationsForUser(command.userId, command.limit, command.offset)
    }

    /**
     * 사용자의 알림을 타입별로 조회합니다.
     * 기본적으로 최신 20개의 알림을 조회합니다. (limit=20, offset=0)
     *
     * @param command 알림 타입별 조회 커맨드
     * @return 알림 목록
     */
    override fun getNotificationsByType(command: GetNotificationsByTypeCommand): List<Notification> {
        return notificationQueryPort.loadNotificationsByType(command.userId, command.type, command.limit, command.offset)
    }

    /**
     * 사용자의 알림을 소스 타입별로 조회합니다.
     * 기본적으로 최신 20개의 알림을 조회합니다. (limit=20, offset=0)
     *
     * @param command 알림 소스별 조회 커맨드
     * @return 알림 목록
     */
    override fun getNotificationsBySource(command: GetNotificationsBySourceCommand): List<Notification> {
        return notificationQueryPort.loadNotificationsBySource(
            command.userId,
            command.sourceType,
            command.sourceId,
            command.limit,
            command.offset
        )
    }

    /**
     * 사용자의 읽지 않은 알림 개수를 조회합니다.
     *
     * @param command 읽지 않은 알림 개수 조회 커맨드
     * @return 읽지 않은 알림 개수
     */
    override fun getUnreadNotificationCount(command: GetUnreadNotificationCountCommand): Int {
        return notificationQueryPort.countUnreadNotifications(command.userId)
    }

}
