package com.stark.shoot.application.service.event.message

import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.notification.NotificationCommandPort
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.application.acl.*
import com.stark.shoot.domain.shared.event.MessagePinEvent
import com.stark.shoot.domain.shared.event.EventVersion
import com.stark.shoot.domain.shared.event.EventVersionValidator
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
 * 메시지 고정 이벤트 리스너
 * - 메시지 고정/해제 시 채팅방 참여자들에게 알림 전송
 */
@ApplicationEventListener
class MessagePinEventListener(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val userQueryPort: UserQueryPort,
    private val notificationCommandPort: NotificationCommandPort,
    private val sendNotificationPort: SendNotificationPort
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 고정 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event MessagePinEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessagePin(event: MessagePinEvent) {
        // Event Version 검증
        EventVersionValidator.checkAndLog(event, EventVersion.MESSAGE_PIN_V1, "MessagePinEventListener")

        logger.info {
            "Processing message pin event: " +
            "messageId=${event.messageId.value}, roomId=${event.roomId.value}, " +
            "isPinned=${event.isPinned}, userId=${event.userId.value}"
        }

        try {
            sendPinNotifications(event)
            logger.info {
                "Message pin event processed successfully: " +
                "messageId=${event.messageId.value}, isPinned=${event.isPinned}"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to process message pin event: " +
                "messageId=${event.messageId.value}"
            }
            // 이벤트 리스너이므로 예외를 삼키고 로그만 남김
        }
    }

    /**
     * 메시지 고정/해제 알림 전송
     * 채팅방 참여자들에게 알림을 보냅니다 (고정한 사용자 제외).
     */
    private fun sendPinNotifications(event: MessagePinEvent) {
        try {
            // 채팅방 정보 조회
            val chatRoom = chatRoomQueryPort.findById(event.roomId.toChatRoom())
            if (chatRoom == null) {
                logger.warn { "ChatRoom not found: roomId=${event.roomId.value}" }
                return
            }

            // 고정한 사용자 정보 조회
            val user = userQueryPort.findUserById(event.userId)
            if (user == null) {
                logger.warn { "User not found: userId=${event.userId.value}" }
                return
            }

            val userNickname = user.nickname.value
            val action = if (event.isPinned) "고정" else "고정 해제"

            // 채팅방 참여자들에게 알림 전송 (고정한 사용자 제외)
            chatRoom.participants
                .filter { it != event.userId }
                .forEach { participantId ->
                    try {
                        val notification = Notification.create(
                            userId = participantId,
                            type = NotificationType.PIN,
                            title = "메시지 ${action}",
                            message = "${userNickname}님이 메시지를 ${action}했습니다.",
                            sourceType = SourceType.CHAT,
                            sourceId = event.messageId.value
                        )

                        // 알림 저장
                        notificationCommandPort.saveNotification(notification)

                        // 실시간 알림 전송 (WebSocket)
                        sendNotificationPort.sendNotification(notification)

                        logger.debug {
                            "Pin notification sent: participant=${participantId.value}, " +
                            "messageId=${event.messageId.value}, isPinned=${event.isPinned}"
                        }
                    } catch (e: Exception) {
                        logger.warn(e) {
                            "Failed to send pin notification to participant: " +
                            "participantId=${participantId.value}"
                        }
                        // 개별 알림 실패는 무시하고 계속 진행
                    }
                }

        } catch (e: Exception) {
            logger.error(e) {
                "Failed to send pin notifications: roomId=${event.roomId.value}"
            }
            throw e
        }
    }
}
