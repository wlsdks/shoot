package com.stark.shoot.application.service.event.friend.request

import com.stark.shoot.domain.shared.event.EventVersion
import com.stark.shoot.domain.shared.event.EventVersionValidator
import com.stark.shoot.application.port.out.notification.NotificationCommandPort
import com.stark.shoot.domain.shared.event.FriendRequestCancelledEvent
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 친구 요청 취소 이벤트 리스너
 * - 친구 요청 취소 시 받은 사람의 알림 정리
 */
@ApplicationEventListener
class FriendRequestCancelledEventListener(
    private val notificationCommandPort: NotificationCommandPort
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 친구 요청 취소 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event FriendRequestCancelledEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleFriendRequestCancelled(event: FriendRequestCancelledEvent) {
        // Event Version 검증
        EventVersionValidator.checkAndLog(event, EventVersion.FRIEND_REQUEST_CANCELLED_V1, "FriendRequestCancelledEventListener")

        logger.info {
            "Processing friend request cancelled event: " +
            "senderId=${event.senderId.value}, receiverId=${event.receiverId.value}"
        }

        try {
            cleanupNotifications(event)
            logger.info {
                "Friend request cancelled event processed successfully: " +
                "senderId=${event.senderId.value}, receiverId=${event.receiverId.value}"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to process friend request cancelled event: " +
                "senderId=${event.senderId.value}, receiverId=${event.receiverId.value}"
            }
            // 이벤트 리스너이므로 예외를 삼키고 로그만 남김
        }
    }

    /**
     * 친구 요청 관련 알림 정리
     * 요청을 받았던 사용자의 알림을 삭제합니다.
     */
    private fun cleanupNotifications(event: FriendRequestCancelledEvent) {
        try {
            // 받은 사람(receiver)의 알림 중 보낸 사람(sender)으로부터 온 친구 요청 알림 삭제
            val deletedCount = notificationCommandPort.deleteNotificationsBySource(
                userId = event.receiverId,
                sourceType = SourceType.USER.name,
                sourceId = event.senderId.value.toString()
            )

            logger.debug {
                "Deleted $deletedCount friend request notifications: " +
                "receiverId=${event.receiverId.value}, senderId=${event.senderId.value}"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to cleanup notifications for cancelled friend request: " +
                "receiverId=${event.receiverId.value}"
            }
            throw e
        }
    }
}
