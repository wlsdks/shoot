package com.stark.shoot.application.service.message.thread

import com.stark.shoot.adapter.`in`.web.dto.message.toDomain
import com.stark.shoot.adapter.`in`.web.dto.message.toRequestDto
import com.stark.shoot.application.port.`in`.message.thread.SendThreadMessageUseCase
import com.stark.shoot.application.port.`in`.message.thread.command.SendThreadMessageCommand
import com.stark.shoot.application.port.out.kafka.KafkaMessagePublishPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.PublishMessagePort
import com.stark.shoot.application.port.out.message.preview.CacheUrlPreviewPort
import com.stark.shoot.application.port.out.message.preview.ExtractUrlPort
import com.stark.shoot.domain.chat.message.service.MessageDomainService
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging

@UseCase
class SendThreadMessageService(
    private val messageQueryPort: MessageQueryPort,
    private val extractUrlPort: ExtractUrlPort,
    private val cacheUrlPreviewPort: CacheUrlPreviewPort,
    private val kafkaMessagePublishPort: KafkaMessagePublishPort,
    private val publishMessagePort: PublishMessagePort,
    private val applicationCoroutineScope: ApplicationCoroutineScope,
    private val messageDomainService: MessageDomainService
) : SendThreadMessageUseCase {

    private val logger = KotlinLogging.logger {}

    override fun sendThreadMessage(command: SendThreadMessageCommand) {
        val request = command.message
        val threadId = MessageId.from(
            request.threadId ?: throw IllegalArgumentException("threadId must not be null")
        )

        messageQueryPort.findById(threadId)
            ?: throw ResourceNotFoundException("스레드 루트 메시지를 찾을 수 없습니다: threadId=$threadId")

        try {
            // 1. 도메인 객체 생성 및 비즈니스 로직 처리
            val domainMessage = messageDomainService.createAndProcessMessage(
                roomId = ChatRoomId.from(request.roomId),
                senderId = UserId.from(request.senderId),
                contentText = request.content.text,
                contentType = request.content.type,
                threadId = request.threadId.let { MessageId.from(it) },
                extractUrls = { text -> extractUrlPort.extractUrls(text) },
                getCachedPreview = { url -> cacheUrlPreviewPort.getCachedUrlPreview(url) }
            )

            // 요청 객체에 도메인 처리 결과 반영
            val metadataDto = domainMessage.metadata.toRequestDto()
            request.tempId = metadataDto.tempId
            request.status = domainMessage.status
            request.metadata.needsUrlPreview = metadataDto.needsUrlPreview
            request.metadata.previewUrl = metadataDto.previewUrl
            request.metadata.urlPreview = metadataDto.urlPreview

            // 2. 메시지 발행 (Redis, Kafka)
            applicationCoroutineScope.launch {
                try {
                    // 1. Redis 발행
                    publishMessagePort.publish(request)

                    // 2. Kafka 발행
                    val chatMessage = request.toDomain()
                    val messageEvent = messageDomainService.createMessageEvent(chatMessage)

                    kafkaMessagePublishPort.publishChatEventSuspend(
                        topic = "chat-messages",
                        key = request.roomId.toString(),
                        event = messageEvent
                    )
                } catch (throwable: Throwable) {
                    logger.error(throwable) { "메시지 처리 실패: roomId=${request.roomId}, content=${request.content.text}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 중 예외 발생: ${e.message}" }
        }
    }

}
