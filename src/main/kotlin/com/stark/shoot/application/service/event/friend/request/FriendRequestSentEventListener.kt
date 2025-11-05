package com.stark.shoot.application.service.event.friend.request

import com.stark.shoot.application.port.out.notification.NotificationCommandPort
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.shared.event.FriendRequestSentEvent
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationMessage
import com.stark.shoot.domain.notification.vo.NotificationTitle
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 친구 요청 전송 이벤트 리스너
 * - 친구 요청을 받은 사용자에게 알림 전송
 */
@ApplicationEventListener
class FriendRequestSentEventListener(
    private val userQueryPort: UserQueryPort,
    private val notificationCommandPort: NotificationCommandPort,
    private val sendNotificationPort: SendNotificationPort
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 친구 요청 전송 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event FriendRequestSentEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleFriendRequestSent(event: FriendRequestSentEvent) {
        logger.info {
            "Processing friend request sent event: " +
            "senderId=${event.senderId.value}, receiverId=${event.receiverId.value}"
        }

        try {
            sendFriendRequestNotification(event)
            logger.info {
                "Friend request sent event processed successfully: " +
                "senderId=${event.senderId.value}, receiverId=${event.receiverId.value}"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to process friend request sent event: " +
                "senderId=${event.senderId.value}, receiverId=${event.receiverId.value}"
            }
            // 이벤트 리스너이므로 예외를 삼키고 로그만 남김
        }
    }

    /**
     * 친구 요청 알림 전송
     * 요청을 받은 사용자에게 알림을 보냅니다.
     */
    private fun sendFriendRequestNotification(event: FriendRequestSentEvent) {
        try {
            val sender = userQueryPort.findUserById(event.senderId)
            if (sender == null) {
                logger.warn { "Sender not found for friend request: senderId=${event.senderId.value}" }
                return
            }

            val senderNickname = sender.nickname.value

            // 알림 생성
            val notification = Notification.create(
                userId = event.receiverId,
                type = NotificationType.FRIEND_REQUEST,
                title = "새로운 친구 요청",
                message = "${senderNickname}님이 친구 요청을 보냈습니다.",
                sourceType = SourceType.USER,
                sourceId = event.senderId.value.toString()
            )

            // 알림 저장
            notificationCommandPort.saveNotification(notification)

            // 실시간 알림 전송 (WebSocket)
            sendNotificationPort.sendNotification(notification)

            logger.debug {
                "Friend request notification sent: " +
                "receiver=${event.receiverId.value}, sender=${event.senderId.value}"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to send friend request notification: " +
                "receiverId=${event.receiverId.value}"
            }
            throw e
        }
    }
}
