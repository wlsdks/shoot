package com.stark.shoot.application.service.event.mention

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.domain.event.MentionEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.concurrent.atomic.AtomicInteger

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
        val mentionNotification = mapOf(
            "type" to "MENTION",
            "roomId" to event.roomId.value,
            "messageId" to event.messageId.value,
            "senderName" to event.senderName,
            "content" to event.messageContent,
            "timestamp" to event.timestamp.toString()
        )

        var successCount = AtomicInteger(0)
        var failureCount = AtomicInteger(0)

        event.mentionedUserIds.forEach { userId ->
            // 개별 사용자에게 멘션 알림 전송
            webSocketMessageBroker.sendMessage(
                "/topic/user/${userId.value}/notifications",
                mentionNotification,
                retryCount = 2 // 멘션은 중요한 알림이므로 재시도
            ).whenComplete { success, throwable ->
                if (success) {
                    successCount.incrementAndGet()
                } else {
                    failureCount.incrementAndGet()
                    logger.warn { "Failed to send mention notification to user ${userId.value}: ${throwable?.message}" }
                }

                // 모든 전솨이 완료되면 로그 출력
                if (successCount.get() + failureCount.get() == event.mentionedUserIds.size) {
                    logger.info { "Mention notifications completed: ${successCount.get()} success, ${failureCount.get()} failure" }
                }
            }
        }
    }

}