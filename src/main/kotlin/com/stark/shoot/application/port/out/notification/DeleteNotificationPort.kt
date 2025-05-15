package com.stark.shoot.application.port.out.notification

interface DeleteNotificationPort {

    /**
     * 특정 알림을 삭제합니다.
     *
     * @param notificationId 알림 ID
     * @return 삭제 성공 여부
     */
    fun deleteNotification(notificationId: String): Boolean

    /**
     * 사용자의 모든 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @return 삭제된 알림 개수
     */
    fun deleteAllNotificationsForUser(userId: Long): Int

    /**
     * 사용자의 특정 타입의 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param type 알림 타입
     * @return 삭제된 알림 개수
     */
    fun deleteNotificationsByType(userId: Long, type: String): Int

    /**
     * 사용자의 특정 소스의 알림을 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param sourceType 소스 타입
     * @param sourceId 소스 ID (null일 경우 모든 소스 ID에 대해 삭제)
     * @return 삭제된 알림 개수
     */
    fun deleteNotificationsBySource(userId: Long, sourceType: String, sourceId: String? = null): Int
}