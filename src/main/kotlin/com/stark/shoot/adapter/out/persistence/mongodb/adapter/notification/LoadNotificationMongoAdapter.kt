package com.stark.shoot.adapter.out.persistence.mongodb.adapter.notification

import com.stark.shoot.adapter.out.persistence.mongodb.repository.NotificationMongoRepository
import com.stark.shoot.application.port.out.notification.LoadNotificationPort
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.vo.NotificationId
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.infrastructure.annotation.Adapter
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@Adapter
class LoadNotificationMongoAdapter(
    private val notificationMongoRepository: NotificationMongoRepository
) : LoadNotificationPort {

    /**
     * ID를 사용하여 알림을 로드합니다.
     *
     * @param id 알림 ID
     * @return 알림 객체 또는 null
     */
    override fun loadNotificationById(id: NotificationId): Notification? {
        return notificationMongoRepository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)
    }

    /**
     * 유저에 대한 알림을 로드합니다.
     *
     * @param userId 사용자 ID
     * @param limit 가져올 알림의 최대 개수
     * @param offset 가져올 알림의 시작 위치
     * @return 알림 목록
     */
    override fun loadNotificationsForUser(
        userId: UserId,
        limit: Int,
        offset: Int
    ): List<Notification> {
        val pageable = PageRequest.of(
            offset / limit,
            limit,
            Sort.by(
                Sort.Direction.DESC,
                "createdAt"
            )
        )

        return notificationMongoRepository.findByUserId(userId.value, pageable)
            .map { it.toDomain() }
    }

    /**
     * 유저에 대한 읽지 않은 알림을 로드합니다.
     *
     * @param userId 사용자 ID
     * @param limit 가져올 알림의 최대 개수
     * @param offset 가져올 알림의 시작 위치
     * @return 읽지 않은 알림 목록
     */
    override fun loadUnreadNotificationsForUser(
        userId: UserId,
        limit: Int,
        offset: Int
    ): List<Notification> {
        val pageable = PageRequest.of(
            offset / limit,
            limit,
            Sort.by(
                Sort.Direction.DESC,
                "createdAt"
            )
        )

        return notificationMongoRepository.findByUserIdAndIsReadFalse(userId.value, pageable)
            .map { it.toDomain() }
    }

    /**
     * 유저에 대한 알림을 타입별로 로드합니다.
     *
     * @param userId 사용자 ID
     * @param type 알림 타입
     * @param limit 가져올 알림의 최대 개수
     * @param offset 가져올 알림의 시작 위치
     * @return 알림 목록
     */
    override fun loadNotificationsByType(
        userId: UserId,
        type: NotificationType,
        limit: Int,
        offset: Int
    ): List<Notification> {
        val pageable = PageRequest.of(
            offset / limit,
            limit,
            Sort.by(
                Sort.Direction.DESC,
                "createdAt"
            )
        )

        return notificationMongoRepository.findByUserIdAndType(userId.value, type.name, pageable)
            .map { it.toDomain() }
    }

    /**
     * 유저에 대한 알림을 소스 타입별로 로드합니다.
     *
     * @param userId 사용자 ID
     * @param sourceType 소스 타입
     * @param sourceId 소스 ID (optional)
     * @param limit 가져올 알림의 최대 개수
     * @param offset 가져올 알림의 시작 위치
     * @return 알림 목록
     */
    override fun loadNotificationsBySource(
        userId: UserId,
        sourceType: SourceType,
        sourceId: String?,
        limit: Int,
        offset: Int
    ): List<Notification> {
        val pageable = PageRequest.of(
            offset / limit,
            limit,
            Sort.by(
                Sort.Direction.DESC,
                "createdAt"
            )
        )

        return if (sourceId != null) {
            notificationMongoRepository.findByUserIdAndSourceTypeAndSourceId(
                userId.value,
                sourceType.name,
                sourceId,
                pageable
            ).map {
                it.toDomain()
            }
        } else {
            notificationMongoRepository
                .findByUserIdAndSourceType(userId.value, sourceType.name, pageable)
                .map { it.toDomain() }
        }
    }

    /**
     * 유저에 대한 읽지 않은 알림 개수를 로드합니다.
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    override fun countUnreadNotifications(userId: UserId): Int {
        return notificationMongoRepository.countByUserIdAndIsReadFalse(userId.value)
    }

}