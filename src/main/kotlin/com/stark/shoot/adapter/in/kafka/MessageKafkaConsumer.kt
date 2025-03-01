package com.stark.shoot.adapter.`in`.kafka

import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class MessageKafkaConsumer(
    private val processMessageUseCase: ProcessMessageUseCase,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    private val logger = KotlinLogging.logger {}

    @KafkaListener(topics = ["chat-messages"], groupId = "shoot-\${random.uuid}")
    fun consumeMessage(@Payload event: ChatEvent) {
        if (event.type == EventType.MESSAGE_CREATED) {
            try {
                // 메시지 저장 및 메타데이터 업데이트
                val savedMessage = processMessageUseCase.processMessage(event.data)

                // 저장된 메시지에서 tempId 추출
                val tempId = event.data.metadata?.get("tempId") as? String

                // tempId가 존재하는 경우에만 상태 업데이트
                if (tempId != null) {
                    // 메시지 저장 성공 상태 업데이트
                    val statusUpdate = MessageStatusResponse(
                        tempId = tempId,
                        status = "saved",
                        persistedId = savedMessage.id,
                        errorMessage = null
                    )

                    // 상태 업데이트 메시지 전송
                    messagingTemplate.convertAndSend("/topic/message/status/${event.data.roomId}", statusUpdate)
                    logger.info { "Message saved with ID: ${savedMessage.id}, updated status for tempId: $tempId" }
                }

            } catch (e: Exception) {
                logger.error(e) { "Error processing message: ${e.message}" }
            }
        }
    }

}