package com.stark.shoot.application.service.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.web.dto.message.toRequestDto
import com.stark.shoot.application.port.out.message.MessagePublisherPort
import com.stark.shoot.application.port.out.message.ScheduledMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.ScheduledMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class ScheduledMessageProcessor(
    private val scheduledMessagePort: ScheduledMessagePort,
    private val messagePublisherPort: MessagePublisherPort,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 10초마다 실행되는 스케줄러
     * 예약 시간이 되었거나 지난 메시지를 처리합니다.
     */
    @Scheduled(fixedRate = 10000) // 10초마다 실행
    fun processScheduledMessages() {
        logger.debug { "예약 메시지 처리 작업 실행" }

        val now = Instant.now()
        val messagesToProcess = scheduledMessagePort.findPendingMessagesBeforeTime(now)

        if (messagesToProcess.isEmpty()) {
            logger.debug { "처리할 예약 메시지가 없습니다" }
            return
        }

        logger.info { "처리할 예약 메시지: ${messagesToProcess.size}개" }

        for (message in messagesToProcess) {
            processMessage(message)
        }
    }

    /**
     * 개별 예약 메시지 처리
     */
    private fun processMessage(message: ScheduledMessage) {
        try {
            // 메시지 요청 객체 생성
            val chatMessageRequest = createChatMessageRequest(message)

            // 도메인 메시지 객체 생성
            val chatMessage = createChatMessage(message)

            // 메시지 발행 (Redis, Kafka)
            messagePublisherPort.publish(chatMessageRequest, chatMessage)

            // 상태 업데이트
            val updatedMessage = message.copy(status = ScheduledMessageStatus.SENT)
            scheduledMessagePort.saveScheduledMessage(updatedMessage)

            logger.info { "예약 메시지 전송 성공: ${message.id}" }
        } catch (e: Exception) {
            logger.error(e) { "예약 메시지 전송 실패: ${message.id}" }
            // 실패한 메시지 처리 방안 (재시도 로직 등) 구현 필요
        }
    }

    /**
     * ScheduledMessage에서 ChatMessageRequest 객체 생성
     */
    private fun createChatMessageRequest(scheduledMessage: ScheduledMessage): ChatMessageRequest {
        return ChatMessageRequest(
            roomId = scheduledMessage.roomId,
            senderId = scheduledMessage.senderId,
            content = MessageContentRequest(
                text = scheduledMessage.content.text,
                type = scheduledMessage.content.type
            ),
            tempId = UUID.randomUUID().toString(),
            metadata = scheduledMessage.metadata.toRequestDto()
        )
    }

    /**
     * ScheduledMessage에서 ChatMessage 도메인 객체 생성
     */
    private fun createChatMessage(scheduledMessage: ScheduledMessage): ChatMessage {
        return ChatMessage(
            id = MessageId.from(UUID.randomUUID().toString()),
            roomId = ChatRoomId.from(scheduledMessage.roomId),
            senderId = UserId.from(scheduledMessage.senderId),
            content = scheduledMessage.content,
            status = MessageStatus.SENDING,
            metadata = scheduledMessage.metadata,
            createdAt = Instant.now()
        )
    }

}
