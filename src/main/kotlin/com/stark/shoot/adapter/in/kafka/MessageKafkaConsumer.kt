package com.stark.shoot.adapter.`in`.kafka

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.HandleMessageEventUseCase
import com.stark.shoot.domain.shared.event.MessageEvent
import com.stark.shoot.domain.shared.event.type.EventType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

/**
 * Kafka에서 메시지를 소비하고 영속화 및 실시간 전달을 처리하는 Consumer
 * 단일 메시지 경로로 MongoDB 저장과 WebSocket 브로드캐스트를 모두 담당합니다.
 */
@Component
class MessageKafkaConsumer(
    private val handleMessageEventUseCase: HandleMessageEventUseCase,
    private val webSocketMessageBroker: WebSocketMessageBroker
) {

    private val logger = KotlinLogging.logger {}

    @KafkaListener(
        topics = ["chat-messages"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "3"  // 병렬 처리 (roomId 파티션 키로 순서 보장)
    )
    fun consumeMessage(
        @Payload event: MessageEvent,
        acknowledgment: Acknowledgment
    ) {
        if (event.type == EventType.MESSAGE_CREATED) {
            try {
                // 1. MongoDB 영속화 먼저 (데이터 일관성 보장)
                val success = handleMessageEventUseCase.handle(event)

                if (success) {
                    // 2. 영속화 성공 후 WebSocket 브로드캐스트 (실시간 전달)
                    broadcastMessage(event)

                    // 3. ACK 처리
                    acknowledgment.acknowledge()
                    logger.debug { "메시지 처리 완료: messageId=${event.data.id?.value}" }
                } else {
                    logger.warn { "메시지 영속화 실패, 재시도 예정: messageId=${event.data.id?.value}" }
                    // 실패 시 ACK하지 않아서 Kafka가 자동 재시도
                    // WebSocket 전송 안했으므로 재시도 시 중복 없음
                }
            } catch (e: Exception) {
                logger.error(e) { "메시지 처리 중 오류 발생: messageId=${event.data.id?.value}" }
                // 예외 발생 시에도 ACK하지 않음 → Kafka 재시도
            }
        }
    }

    /**
     * WebSocket을 통해 메시지를 구독자들에게 즉시 브로드캐스트합니다.
     */
    private fun broadcastMessage(event: MessageEvent) {
        try {
            val roomId = event.data.roomId.value
            webSocketMessageBroker.sendMessage(
                "/topic/messages/$roomId",
                event.data
            )
            logger.debug { "메시지 브로드캐스트 완료: roomId=$roomId, messageId=${event.data.id?.value}" }
        } catch (e: Exception) {
            logger.error(e) { "메시지 브로드캐스트 실패: ${event.data.id?.value}" }
            // 브로드캐스트 실패는 영속화와 무관하므로 로그만 남김
        }
    }
}
