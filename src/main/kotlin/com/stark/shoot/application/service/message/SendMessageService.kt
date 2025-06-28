package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.toRequestDto
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.SendMessageCommand
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageDomainService
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.application.service.message.MessagePublisher
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging


@UseCase
class SendMessageService(
    private val extractUrlPort: ExtractUrlPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val messagePublisher: MessagePublisher,
    private val messageDomainService: MessageDomainService
) : SendMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 전송합니다.
     * 1. 도메인 객체 생성 및 비즈니스 로직 처리
     * 2. 메시지 발행 (Redis, Kafka)
     *
     * @param command 메시지 전송 커맨드
     */
    override fun sendMessage(command: SendMessageCommand) {
        val messageRequest = command.message
        try {
            // 1. 도메인 객체 생성 및 비즈니스 로직 처리
            val domainMessage = createAndProcessDomainMessage(messageRequest)

            // 2. 메시지 발행 (Redis, Kafka)
            messagePublisher.publish(messageRequest, domainMessage)
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 중 예외 발생: ${e.message}" }
            messagePublisher.handleProcessingError(messageRequest, e)
        }
    }

    /**
     * 도메인 메시지 객체를 생성하고 비즈니스 로직을 처리합니다.
     *
     * @param messageRequest 메시지 요청 DTO
     * @return 처리된 도메인 메시지 객체
     */
    private fun createAndProcessDomainMessage(
        messageRequest: ChatMessageRequest
    ): ChatMessage {
        val messageWithPreview = messageDomainService.createAndProcessMessage(
            roomId = ChatRoomId.from(messageRequest.roomId),
            senderId = UserId.from(messageRequest.senderId),
            contentText = messageRequest.content.text,
            contentType = messageRequest.content.type,
            threadId = messageRequest.threadId?.let { MessageId.from(it) },
            extractUrls = { text -> extractUrlPort.extractUrls(text) },
            getCachedPreview = { url -> cacheUrlPreviewPort.getCachedUrlPreview(url) }
        )

        // 요청 객체에 도메인 처리 결과 반영
        val metadataDto = messageWithPreview.metadata.toRequestDto()
        messageRequest.tempId = metadataDto.tempId
        messageRequest.status = messageWithPreview.status
        messageRequest.metadata.needsUrlPreview = metadataDto.needsUrlPreview
        messageRequest.metadata.previewUrl = metadataDto.previewUrl
        messageRequest.metadata.urlPreview = metadataDto.urlPreview

        return messageWithPreview
    }

}
