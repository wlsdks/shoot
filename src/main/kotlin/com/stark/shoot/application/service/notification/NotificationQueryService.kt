package com.stark.shoot.application.service.notification

import com.stark.shoot.application.port.`in`.notification.NotificationQueryUseCase
import com.stark.shoot.application.port.out.notification.LoadNotificationPort
import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import org.springframework.stereotype.Service

@Service
class NotificationQueryService(
    private val loadNotificationPort: LoadNotificationPort
) : NotificationQueryUseCase {

    /**
     * 유저에 대한 알림을 가져옵니다.
     * 기본적으로 최신 20개의 알림을 조회합니다. (limit=20, offset=0)
     *
     * @param userId 사용자 ID
     * @param limit 가져올 알림의 최대 개수
     * @param offset 가져올 알림의 시작 위치
     * @return 알림 목록
     */
    override fun getNotificationsForUser(
        userId: UserId,
        limit: Int,
        offset: Int
    ): List<Notification> {
        return loadNotificationPort.loadNotificationsForUser(userId, limit, offset)
    }

    /**
     * 유저에 대한 읽지 않은 알림을 가져옵니다.
     * 기본적으로 최신 20개의 읽지 않은 알림을 조회합니다. (limit=20, offset=0)
     *
     * @param userId 사용자 ID
     * @param limit 가져올 알림의 최대 개수
     * @param offset 가져올 알림의 시작 위치
     * @return 읽지 않은 알림 목록
     */
    override fun getUnreadNotificationsForUser(
        userId: UserId,
        limit: Int,
        offset: Int
    ): List<Notification> {
        return loadNotificationPort.loadUnreadNotificationsForUser(userId, limit, offset)
    }

    /**
     * 사용자의 알림을 타입별로 조회합니다.
     * 기본적으로 최신 20개의 알림을 조회합니다. (limit=20, offset=0)
     *
     * @param userId 사용자 ID
     * @param type 알림 타입
     * @param limit 가져올 알림의 최대 개수
     * @param offset 가져올 알림의 시작 위치
     * @return 알림 목록
     */
    override fun getNotificationsByType(
        userId: UserId,
        type: NotificationType,
        limit: Int,
        offset: Int
    ): List<Notification> {
        return loadNotificationPort.loadNotificationsByType(userId, type, limit, offset)
    }

    /**
     * 사용자의 알림을 소스 타입별로 조회합니다.
     * 기본적으로 최신 20개의 알림을 조회합니다. (limit=20, offset=0)
     *
     * @param userId 사용자 ID
     * @param sourceType 소스 타입
     * @param sourceId 소스 ID (null일 경우 모든 소스 ID에 대해 조회)
     * @param limit 가져올 알림의 최대 개수
     * @param offset 가져올 알림의 시작 위치
     * @return 알림 목록
     */
    override fun getNotificationsBySource(
        userId: UserId,
        sourceType: SourceType,
        sourceId: String?,
        limit: Int,
        offset: Int
    ): List<Notification> {
        return loadNotificationPort.loadNotificationsBySource(userId, sourceType, sourceId, limit, offset)
    }

    /**
     * 사용자의 읽지 않은 알림 개수를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    override fun getUnreadNotificationCount(userId: UserId): Int {
        return loadNotificationPort.countUnreadNotifications(userId)
    }

}