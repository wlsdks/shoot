package com.stark.shoot.application.service.event.friend

import com.stark.shoot.application.port.`in`.chatroom.CreateChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.CreateDirectChatCommand
import com.stark.shoot.application.port.out.notification.NotificationCommandPort
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.event.FriendAddedEvent
import com.stark.shoot.domain.notification.Notification
import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.notification.vo.NotificationMessage
import com.stark.shoot.domain.notification.vo.NotificationTitle
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 친구 추가 이벤트 리스너
 * - 친구 추가 시 1:1 채팅방 자동 생성
 * - 양쪽 사용자에게 친구 추가 알림 전송
 */
@ApplicationEventListener
class FriendAddedEventListener(
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val userQueryPort: UserQueryPort,
    private val notificationCommandPort: NotificationCommandPort,
    private val sendNotificationPort: SendNotificationPort
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 친구 추가 이벤트 처리
     * 트랜잭션 커밋 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event FriendAddedEvent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleFriendAdded(event: FriendAddedEvent) {
        logger.info { "Processing friend added event: userId=${event.userId.value}, friendId=${event.friendId.value}" }

        try {
            // 1. 1:1 채팅방 자동 생성
            createDirectChatRoom(event)

            // 2. 친구 추가 알림 전송
            sendFriendAcceptedNotification(event)

            logger.info { "Friend added event processed successfully: userId=${event.userId.value}, friendId=${event.friendId.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to process friend added event: userId=${event.userId.value}, friendId=${event.friendId.value}" }
            // 이벤트 리스너이므로 예외를 삼키고 로그만 남김 (다른 리스너 실행에 영향 없도록)
        }
    }

    /**
     * 1:1 채팅방 자동 생성
     * 이미 존재하는 경우 새로 생성하지 않음
     */
    private fun createDirectChatRoom(event: FriendAddedEvent) {
        try {
            val command = CreateDirectChatCommand(
                userId = event.userId,
                friendId = event.friendId
            )

            val chatRoom = createChatRoomUseCase.createDirectChat(command)
            logger.debug { "Direct chat room created/retrieved: roomId=${chatRoom.roomId}, userId=${event.userId.value}, friendId=${event.friendId.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to create direct chat room for friends: userId=${event.userId.value}, friendId=${event.friendId.value}" }
            throw e
        }
    }

    /**
     * 친구 추가 알림 전송
     * 양쪽 사용자 모두에게 알림 전송
     */
    private fun sendFriendAcceptedNotification(event: FriendAddedEvent) {
        try {
            // 사용자 정보 조회
            val user = userQueryPort.findUserById(event.userId)
            val friend = userQueryPort.findUserById(event.friendId)

            if (user == null || friend == null) {
                logger.warn { "User or friend not found for notification: userId=${event.userId.value}, friendId=${event.friendId.value}" }
                return
            }

            // 두 개의 알림 생성 (양방향)
            val notifications = listOf(
                // 친구에게 보낼 알림 (userId가 friendId에게 보내는 알림)
                createFriendAcceptedNotification(
                    recipientId = event.friendId,
                    friendName = user.nickname.value,
                    sourceId = event.userId.value.toString()
                ),
                // 사용자에게 보낼 알림 (friendId가 userId에게 보내는 알림)
                createFriendAcceptedNotification(
                    recipientId = event.userId,
                    friendName = friend.nickname.value,
                    sourceId = event.friendId.value.toString()
                )
            )

            // 알림 저장
            val savedNotifications = notificationCommandPort.saveNotifications(notifications)
            logger.debug { "Friend accepted notifications saved: count=${savedNotifications.size}" }

            // 알림 전송
            sendNotificationPort.sendNotifications(savedNotifications)
            logger.debug { "Friend accepted notifications sent: count=${savedNotifications.size}" }

        } catch (e: Exception) {
            logger.error(e) { "Failed to send friend accepted notification: userId=${event.userId.value}, friendId=${event.friendId.value}" }
            // 알림 전송 실패는 치명적이지 않으므로 예외를 삼킴
        }
    }

    /**
     * 친구 수락 알림 생성
     */
    private fun createFriendAcceptedNotification(
        recipientId: com.stark.shoot.domain.shared.UserId,
        friendName: String,
        sourceId: String
    ): Notification {
        return Notification(
            userId = recipientId,
            title = NotificationTitle.from("친구 추가"),
            message = NotificationMessage.from("${friendName}님과 친구가 되었습니다"),
            type = NotificationType.FRIEND_ACCEPTED,
            sourceId = sourceId,
            sourceType = SourceType.FRIEND,
            metadata = mapOf(
                "friendName" to friendName
            )
        )
    }
}
