package com.stark.shoot.domain.notification

import com.stark.shoot.domain.exception.NotificationException
import java.time.Instant

data class Notification(
    val id: String? = null,
    val userId: Long,
    val title: String,
    val message: String,
    val type: NotificationType,
    val sourceId: String,
    val sourceType: SourceType,
    val isRead: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val readAt: Instant? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val isDeleted: Boolean = false,
    val deletedAt: Instant? = null
) {

    /**
     * 알림을 읽음 처리합니다.
     *
     * @return 읽음 처리된 알림 객체
     */
    fun markAsRead(): Notification {
        return if (isRead) {
            this
        } else {
            this.copy(isRead = true, readAt = Instant.now())
        }
    }

    /**
     * 알림이 특정 사용자에게 속하는지 확인합니다.
     *
     * @param userId 확인할 사용자 ID
     * @return 사용자에게 속하면 true, 아니면 false
     */
    fun belongsToUser(userId: Long): Boolean {
        return this.userId == userId
    }

    /**
     * 사용자 소유권을 확인하고, 소유자가 아니면 예외를 발생시킵니다.
     *
     * @param userId 확인할 사용자 ID
     * @throws NotificationException 알림이 해당 사용자의 것이 아닌 경우
     */
    fun validateOwnership(userId: Long) {
        if (!belongsToUser(userId)) {
            throw NotificationException("알림이 해당 사용자의 것이 아닙니다: ${this.id}", "NOTIFICATION_OWNERSHIP_INVALID")
        }
    }

    /**
     * 알림을 소프트 삭제 처리합니다.
     *
     * @return 삭제 처리된 알림 객체
     */
    fun markAsDeleted(): Notification {
        return if (isDeleted) {
            this
        } else {
            this.copy(isDeleted = true, deletedAt = Instant.now())
        }
    }


    companion object {
        /**
         * 채팅 이벤트로부터 알림을 생성합니다.
         *
         * @param userId 사용자 ID
         * @param title 알림 제목
         * @param message 알림 메시지
         * @param type 알림 타입
         * @param sourceId 소스 ID
         * @param metadata 메타데이터
         * @return 생성된 알림 객체
         */
        fun fromChatEvent(
            userId: Long,
            title: String,
            message: String,
            type: NotificationType,
            sourceId: String,
            metadata: Map<String, Any> = emptyMap()
        ): Notification {
            return Notification(
                userId = userId,
                title = title,
                message = message,
                type = type,
                sourceId = sourceId,
                sourceType = SourceType.CHAT,
                metadata = metadata
            )
        }

    }
}
