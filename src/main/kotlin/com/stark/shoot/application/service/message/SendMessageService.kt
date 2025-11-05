package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.updateFromDomain
import com.stark.shoot.application.port.`in`.message.SendMessageUseCase
import com.stark.shoot.application.port.`in`.message.command.SendMessageCommand
import com.stark.shoot.application.port.out.message.MessagePublisherPort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.service.MessageDomainService
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging


@UseCase
class SendMessageService(
    private val extractUrlPort: ExtractUrlPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val messagePublisherPort: MessagePublisherPort,
    private val messageDomainService: MessageDomainService
) : SendMessageUseCase {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지를 전송합니다.
     * 1. 도메인 객체 생성 및 비즈니스 로직 처리
     * 2. 메시지 발행 (Redis, Kafka)
     *
     * @param command 메시지 전송 커맨드
     * @see com.stark.shoot.adapter.in.redis.MessageRedisStreamListener redis 스트림 리스너
     * @see HandleMessageEventService Kafka 메시지 처리 서비스
     */
    override fun sendMessage(command: SendMessageCommand) {
        val messageRequest = command.message

        runCatching {
            // 1. 도메인 객체 생성 및 비즈니스 로직 처리
            createAndProcessDomainMessage(messageRequest)
        }.onSuccess { domainMessage ->
            // 2. 메시지 발행 (Redis, Kafka)
            messagePublisherPort.publish(messageRequest, domainMessage)
        }.onFailure { throwable ->
            logger.error(throwable) { "메시지 처리 중 예외 발생: ${throwable.message}" }
            messagePublisherPort.handleProcessingError(messageRequest, throwable)
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
            tempId = messageRequest.tempId, // front에서 받은 기존 tempId 전달
            threadId = messageRequest.threadId?.let { MessageId.from(it) },
            extractUrls = { text -> extractUrlPort.extractUrls(text) },
            getCachedPreview = { url -> cacheUrlPreviewPort.getCachedUrlPreview(url) }
        )

        // 요청 객체에 도메인 처리 결과 반영
        messageRequest.updateFromDomain(messageWithPreview)

        return messageWithPreview
    }

}
