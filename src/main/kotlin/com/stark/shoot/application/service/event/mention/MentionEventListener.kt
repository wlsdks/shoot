package com.stark.shoot.application.service.event.mention

import com.stark.shoot.adapter.`in`.rest.socket.WebSocketMessageBroker
import com.stark.shoot.domain.event.MentionEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.event.TransactionPhase

@ApplicationEventListener
class MentionEventListener(
    private val webSocketMessageBroker: WebSocketMessageBroker
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 멘션 이벤트 처리 - 멘션된 사용자들에게 개별 알림 전송
     * 트랜잭션 커밋 후에 실행되어 데이터 일관성을 보장합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMention(event: MentionEvent) {
        event.mentionedUserIds.forEach { userId ->
            val mentionNotification = mapOf(
                "type" to "MENTION",
                "roomId" to event.roomId.value,
                "messageId" to event.messageId.value,
                "senderName" to event.senderName,
                "content" to event.messageContent,
                "timestamp" to event.timestamp.toString()
            )

            // 개별 사용자에게 멘션 알림 전송
            webSocketMessageBroker.sendMessage(
                "/topic/user/${userId.value}/notifications",
                mentionNotification
            )
        }

        logger.info { "Mention notifications sent to ${event.mentionedUserIds.size} users" }
    }

}