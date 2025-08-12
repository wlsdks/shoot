package com.stark.shoot.application.service.event.reaction

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.domain.event.MessageReactionEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@ApplicationEventListener
class ReactionEventListener(
    private val webSocketMessageBroker: WebSocketMessageBroker
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 반응 이벤트 처리 - 채팅방에 반응 업데이트 브로드캐스트
     * 트랜잭션 커밋 후에 실행되어 데이터 일관성을 보장합니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessageReaction(event: MessageReactionEvent) {
        val reactionUpdate = mapOf(
            "messageId" to event.messageId.value,
            "userId" to event.userId.value,
            "reactionType" to event.reactionType,
            "isAdded" to event.isAdded
        )

        // 채팅방의 모든 사용자에게 반응 업데이트 전송
        webSocketMessageBroker.sendMessage(
            "/topic/chat/${event.roomId.value}/reactions",
            reactionUpdate,
            retryCount = 1 // 반응은 실시간성이 중요하므로 재시도 최소화
        ).whenComplete { success, throwable ->
            if (success) {
                logger.debug { "Reaction update sent for message ${event.messageId.value}" }
            } else {
                logger.warn { "Failed to send reaction update for message ${event.messageId.value}: ${throwable?.message}" }
            }
        }
    }

}