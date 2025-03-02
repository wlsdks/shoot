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
                // 메시지 내부의 임시 ID, 채팅방 ID 추출
                val tempId = event.data.metadata["tempId"] as? String ?: return
                val roomId = event.data.roomId

                // MongoDB 저장 전 처리 중 상태 업데이트
                sendStatusUpdate(roomId, tempId, "processing", null)

                // 메시지 저장
                val savedMessage = processMessageUseCase.processMessage(event.data)

                // 저장 성공 상태 업데이트
                sendStatusUpdate(roomId, tempId, "saved", savedMessage.id)

            } catch (e: Exception) {
                val tempId = event.data.metadata["tempId"] as? String
                if (tempId != null) {
                    sendStatusUpdate(event.data.roomId, tempId, "failed", null, e.message)
                }
                logger.error(e) { "메시지 처리 오류: ${e.message}" }
            }
        }
    }

    /**
     * 메시지 상태 업데이트를 WebSocket으로 전송합니다.
     *
     * @param roomId 채팅방 ID
     * @param tempId 임시 메시지 ID
     * @param status 상태: "sending", "saved", "failed" 등
     * @param persistedId 영구 저장된 메시지 ID (성공 시)
     * @param errorMessage 오류 메시지 (실패 시)
     */
    private fun sendStatusUpdate(
        roomId: String,
        tempId: String,
        status: String,
        persistedId: String?,
        errorMessage: String? = null
    ) {
        val statusUpdate = MessageStatusResponse(tempId, status, persistedId, errorMessage)
        messagingTemplate.convertAndSend("/topic/message/status/$roomId", statusUpdate)
    }

}