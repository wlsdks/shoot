package com.stark.shoot.domain.notification

import com.stark.shoot.domain.exception.NotificationException
import com.stark.shoot.domain.notification.event.NotificationEvent
import com.stark.shoot.domain.notification.NotificationTitle
import com.stark.shoot.domain.notification.NotificationId
import com.stark.shoot.domain.notification.NotificationMessage
import java.time.Instant

/**
 * 알림 도메인 엔티티
 *
 * 알림은 사용자에게 전달되는 메시지로, 다양한 소스(채팅, 친구 요청 등)에서 발생할 수 있습니다.
 * 알림은 읽음 상태와 삭제 상태를 가지며, 이러한 상태 변경은 도메인 로직을 통해 이루어집니다.
 */
class Notification(
    val id: NotificationId? = null,
    val userId: Long,
    val title: NotificationTitle,
    val message: NotificationMessage,
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
        if (isRead) {
            return this
        }

        return Notification(
            id = this.id,
            userId = this.userId,
            title = this.title,
            message = this.message,
            type = this.type,
            sourceId = this.sourceId,
            sourceType = this.sourceType,
            isRead = true,
            createdAt = this.createdAt,
            readAt = Instant.now(),
            metadata = this.metadata,
            isDeleted = this.isDeleted,
            deletedAt = this.deletedAt
        )
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
        if (isDeleted) {
            return this
        }

        return Notification(
            id = this.id,
            userId = this.userId,
            title = this.title,
            message = this.message,
            type = this.type,
            sourceId = this.sourceId,
            sourceType = this.sourceType,
            isRead = this.isRead,
            createdAt = this.createdAt,
            readAt = this.readAt,
            metadata = this.metadata,
            isDeleted = true,
            deletedAt = Instant.now()
        )
    }

    companion object Factory {
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
                title = NotificationTitle.from(title),
                message = NotificationMessage.from(message),
                type = type,
                sourceId = sourceId,
                sourceType = SourceType.CHAT,
                metadata = metadata
            )
        }

        /**
         * 알림 이벤트로부터 알림을 생성합니다.
         *
         * @param event 알림 이벤트
         * @param recipientId 수신자 ID
         * @return 생성된 알림 객체
         */
        fun fromEvent(event: NotificationEvent, recipientId: Long): Notification {
            return Notification(
                userId = recipientId,
                title = NotificationTitle.from(event.getTitle()),
                message = NotificationMessage.from(event.getMessage()),
                type = event.type,
                sourceId = event.sourceId,
                sourceType = event.sourceType,
                metadata = event.metadata
            )
        }

        /**
         * 알림을 생성합니다.
         *
         * @param userId 사용자 ID
         * @param title 알림 제목
         * @param message 알림 메시지
         * @param type 알림 타입
         * @param sourceId 소스 ID
         * @param sourceType 소스 타입
         * @param metadata 메타데이터
         * @return 생성된 알림 객체
         */
        fun create(
            userId: Long,
            title: String,
            message: String,
            type: NotificationType,
            sourceId: String,
            sourceType: SourceType,
            metadata: Map<String, Any> = emptyMap()
        ): Notification {
            return Notification(
                userId = userId,
                title = NotificationTitle.from(title),
                message = NotificationMessage.from(message),
                type = type,
                sourceId = sourceId,
                sourceType = sourceType,
                metadata = metadata
            )
        }
    }
}
