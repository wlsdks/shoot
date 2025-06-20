package com.stark.shoot.application.port.out.notification

import com.stark.shoot.domain.common.vo.UserId
import com.stark.shoot.domain.notification.NotificationId
import com.stark.shoot.infrastructure.exception.web.MongoOperationException
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

interface DeleteNotificationPort {

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param notificationId 알림 ID
     * @throws ResourceNotFoundException 알림을 찾을 수 없는 경우
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    fun deleteNotification(notificationId: NotificationId)

    /**
     * 사용자의 모든 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @return 삭제된 알림 개수 (성공 시)
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    fun deleteAllNotificationsForUser(userId: UserId): Int

    /**
     * 사용자의 특정 타입의 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param type 알림 타입
     * @return 삭제된 알림 개수 (성공 시)
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    fun deleteNotificationsByType(userId: UserId, type: String): Int

    /**
     * 사용자의 특정 소스의 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param sourceType 소스 타입
     * @param sourceId 소스 ID (null일 경우 모든 소스 ID에 대해 삭제)
     * @return 삭제된 알림 개수 (성공 시)
     * @throws MongoOperationException 데이터베이스 작업 실패 시
     */
    fun deleteNotificationsBySource(userId: UserId, sourceType: String, sourceId: String? = null): Int
}
