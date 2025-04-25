package com.stark.shoot.adapter.`in`.kafka

import com.stark.shoot.adapter.`in`.web.dto.message.MessageStatusResponse
import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.embedded.type.MessageStatus
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.LoadUrlContentPort
import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.UrlPreview
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MessageKafkaConsumer(
    private val processMessageUseCase: ProcessMessageUseCase,
    private val loadUrlContentPort: LoadUrlContentPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val webSocketMessageBroker: WebSocketMessageBroker,
    private val chatMessageMapper: ChatMessageMapper
) {
    private val logger = KotlinLogging.logger {}

    @KafkaListener(
        topics = ["chat-messages"],
        groupId = "\${spring.kafka.consumer.group-id}", // 설정 파일의 값을 사용
        containerFactory = "kafkaListenerContainerFactory" // 명시적으로 컨테이너 팩토리 지정
    )
    fun consumeMessage(
        @Payload event: ChatEvent,
        acknowledgment: Acknowledgment
    ) {
        if (event.type == EventType.MESSAGE_CREATED) {
            // 코루틴 내부에서 비동기 처리
            runBlocking {
                try {
                    // 메시지 내부의 임시 ID, 채팅방 ID 추출
                    val tempId = event.data.metadata.tempId!!
                    val roomId = event.data.roomId

                    // MongoDB 저장 전 처리 중 상태 업데이트
                    sendStatusUpdate(roomId, tempId, MessageStatus.PROCESSING.name, null)

                    // 메시지 저장
                    val savedMessage = processMessageUseCase.processMessageCreate(event.data)

                    // 저장 성공 상태 업데이트
                    sendStatusUpdate(roomId, tempId, MessageStatus.SAVED.name, savedMessage.id)

                    // URL 미리보기 처리 필요 여부 확인
                    processChatMessageForUrlPreview(savedMessage)

                    // 처리 완료 후 수동 커밋
                    acknowledgment.acknowledge()
                } catch (e: Exception) {
                    sendErrorResponse(event, e)
                }
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
        roomId: Long,
        tempId: String,
        status: String,
        persistedId: String?,
        errorMessage: String? = null
    ) {
        // 현재 시간을 ISO 형식 문자열로 변환
        val createdAt = Instant.now().toString()

        val statusUpdate = MessageStatusResponse(
            tempId = tempId,
            status = status,
            persistedId = persistedId,
            errorMessage = errorMessage,
            createdAt = createdAt
        )

        webSocketMessageBroker.sendMessage("/topic/message/status/$roomId", statusUpdate)
    }

    /**
     * 메시지 처리 중 오류 발생 시 클라이언트에 에러 메시지를 전송합니다.
     *
     * @param event ChatEvent 객체
     * @param e Exception 객체
     */
    private fun sendErrorResponse(
        event: ChatEvent,
        e: Exception
    ) {
        val tempId = event.data.metadata.tempId

        if (tempId != null) {
            sendStatusUpdate(
                event.data.roomId,
                tempId,
                MessageStatus.FAILED.name,
                null,
                e.message
            )
        }

        logger.error(e) { "메시지 처리 오류: ${e.message}" }
    }

    /**
     * URL 미리보기를 처리합니다.
     * 메시지의 메타데이터에 URL 미리보기가 필요하다고 표시된 경우,
     * URL을 비동기적으로 가져와서 메시지를 업데이트합니다.
     *
     * @param savedMessage 저장된 메시지
     */
    private fun processChatMessageForUrlPreview(savedMessage: ChatMessage) {
        if (savedMessage.metadata.needsUrlPreview &&
            savedMessage.metadata.previewUrl != null
        ) {
            val previewUrl = savedMessage.metadata.previewUrl!!

            // URL 미리보기 비동기 처리
            try {
                val preview = loadUrlContentPort.fetchUrlContent(previewUrl)
                if (preview != null) {
                    // 캐싱
                    cacheUrlPreviewPort.cacheUrlPreview(previewUrl, preview)

                    // 메시지 업데이트
                    val updatedMessage = updateMessageWithPreview(savedMessage, preview)

                    // 업데이트된 메시지 전송 (URL 정보가 저장되면 화면에 실시간 업데이트)
                    sendMessageUpdate(updatedMessage)
                }
            } catch (e: Exception) {
                logger.error(e) { "URL 미리보기 처리 실패: $previewUrl" }
            }
        }
    }

    /**
     * 메시지에 URL 미리보기 정보를 추가합니다.
     *
     * @param message 메시지
     * @param preview URL 미리보기 정보
     * @return 업데이트된 메시지
     */
    private fun updateMessageWithPreview(
        message: ChatMessage,
        preview: UrlPreview
    ): ChatMessage {
        val updatedMetadata = message.metadata
        val currentContent = message.content
        val currentMetadata = currentContent.metadata ?: ChatMessageMetadata()
        val updatedContentMetadata = currentMetadata.copy(urlPreview = preview)
        val updatedContent = currentContent.copy(metadata = updatedContentMetadata)

        // 업데이트된 메시지
        return message.copy(
            content = updatedContent,
            metadata = updatedMetadata
        )
    }

    /**
     * 수정된 메시지를 WebSocket으로 전송합니다.
     * 이 메서드는 url 추출이 시간이 조금 걸리니 사용자 화면에 추출이 완료되면 업데이트 하기 위해 사용합니다.
     *
     * @param message 수정된 메시지
     */
    private fun sendMessageUpdate(message: ChatMessage) {
        // 수정: ChatMessageMapper를 주입 받아야 함
        val messageDto = chatMessageMapper.toDto(message)

        // WebSocket으로 전송
        webSocketMessageBroker.sendMessage(
            "/topic/message/update/${message.roomId}",
            messageDto
        )
    }

}
