package com.stark.shoot.adapter.`in`.kafka

import com.stark.shoot.application.port.`in`.message.HandleMessageEventUseCase
import com.stark.shoot.domain.event.MessageEvent
import com.stark.shoot.domain.event.type.EventType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class MessageKafkaConsumer(
    private val handleMessageEventUseCase: HandleMessageEventUseCase
) {

    @KafkaListener(
        topics = ["chat-messages"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeMessage(
        @Payload event: MessageEvent,
        acknowledgment: Acknowledgment
    ) {
        if (event.type == EventType.MESSAGE_CREATED) {
            val success = handleMessageEventUseCase.handle(event)
            if (success) {
                acknowledgment.acknowledge()
            }
        }
    }

}
