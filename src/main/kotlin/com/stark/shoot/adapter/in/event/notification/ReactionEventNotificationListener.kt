package com.stark.shoot.adapter.`in`.event.notification

import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.notification.SaveNotificationPort
import com.stark.shoot.application.port.out.notification.SendNotificationPort
import com.stark.shoot.domain.chat.event.MessageReactionEvent
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 메시지 반응 이벤트를 수신하여 알림을 생성하고 전송하는 리스너 클래스입니다.
 *
 * 이 클래스는 메시지 반응 이벤트를 수신하여 다음과 같은 작업을 수행합니다:
 * 1. 메시지 작성자 식별
 * 2. 반응에 대한 알림 생성
 * 3. 알림 저장 및 전송
 */
@Component
class ReactionEventNotificationListener(
    private val loadMessagePort: LoadMessagePort,
    private val saveNotificationPort: SaveNotificationPort,
    private val sendNotificationPort: SendNotificationPort,
    private val chatNotificationFactory: ChatNotificationFactory
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 반응 이벤트를 수신하여 알림 처리를 시작하는 메서드입니다.
     *
     * @param event 처리할 메시지 반응 이벤트
     */
    @EventListener
    fun handleReactionEvent(event: MessageReactionEvent) {
        try {
            // 메시지 조회
            val message = loadMessagePort.findById(event.messageId.toObjectId())
                ?: run {
                    logger.warn { "메시지를 찾을 수 없습니다: messageId=${event.messageId}" }
                    return
                }

            // 메시지 작성자가 반응을 추가한 사용자와 같으면 알림을 보내지 않음
            val reactingUserId = event.userId.toLong()
            if (message.senderId == reactingUserId) {
                logger.info { "자신의 메시지에 대한 반응은 알림을 생성하지 않습니다: messageId=${event.messageId}" }
                return
            }

            // 반응이 제거된 경우 알림을 생성하지 않음
            if (!event.isAdded) {
                if (event.isReplacement) {
                    logger.info { "리액션 교체 중 제거 이벤트이므로 알림을 생성하지 않습니다: messageId=${event.messageId}" }
                } else {
                    logger.info { "반응 제거는 알림을 생성하지 않습니다: messageId=${event.messageId}" }
                }
                return
            }

            // 리액션 교체의 일부인 경우, 추가 이벤트에 대해서만 알림을 생성함
            if (event.isReplacement && event.isAdded) {
                logger.info { "리액션 교체 중 추가 이벤트에 대해 알림을 생성합니다: messageId=${event.messageId}" }
            }

            // 알림 생성
            val notification = createReactionNotification(
                userId = message.senderId,
                reactingUserId = reactingUserId,
                messageId = event.messageId,
                roomId = event.roomId,
                reactionType = event.reactionType
            )

            // 알림 저장 및 전송
            val savedNotification = saveNotificationPort.saveNotification(notification)
            sendNotificationPort.sendNotification(savedNotification)

            logger.info { "메시지 반응 알림이 생성되고 전송되었습니다: messageId=${event.messageId}, userId=${message.senderId}" }

        } catch (e: Exception) {
            logger.error(e) { "메시지 반응 이벤트 처리 중 오류가 발생했습니다: ${e.message}" }
        }
    }

    /**
     * 메시지 반응에 대한 알림을 생성합니다.
     *
     * @param userId 알림을 받을 사용자 ID (메시지 작성자)
     * @param reactingUserId 반응을 추가한 사용자 ID
     * @param messageId 메시지 ID
     * @param roomId 채팅방 ID
     * @param reactionType 반응 타입
     * @return 생성된 반응 알림
     */
    private fun createReactionNotification(
        userId: Long,
        reactingUserId: Long,
        messageId: String,
        roomId: String,
        reactionType: String
    ) = chatNotificationFactory.createReactionNotification(
        userId = userId,
        reactingUserId = reactingUserId,
        messageId = messageId,
        roomId = roomId,
        reactionType = reactionType
    )
}
