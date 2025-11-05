package com.stark.shoot.application.service.message.schedule

import com.stark.shoot.adapter.`in`.rest.dto.ApiException
import com.stark.shoot.adapter.`in`.rest.dto.ErrorCode
import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.rest.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.message.toRequestDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ScheduledMessageMapper
import com.stark.shoot.application.port.`in`.message.schedule.ScheduledMessageUseCase
import com.stark.shoot.application.port.`in`.message.schedule.command.*
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.MessagePublisherPort
import com.stark.shoot.application.port.out.message.ScheduledMessagePort
import com.stark.shoot.application.acl.*
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.ScheduledMessage
import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.UUID

@UseCase
class ScheduledMessageService(
    private val scheduledMessagePort: ScheduledMessagePort,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val scheduledMessageMapper: ScheduledMessageMapper,
    private val messagePublisherPort: MessagePublisherPort,
) : ScheduledMessageUseCase {

    private val logger = KotlinLogging.logger {}

    override fun scheduleMessage(command: ScheduleMessageCommand): ScheduledMessageResponseDto {
        val roomId = command.roomId
        val senderId = command.senderId
        val content = command.content
        val scheduledAt = command.scheduledAt

        // 채팅방 존재여부 확인
        val chatRoom = (chatRoomQueryPort.findById(roomId)
            ?: throw ApiException("채팅방을 찾을 수 없습니다.", ErrorCode.ROOM_NOT_FOUND))

        // 사용자가 채팅방에 속해있는지 확인
        if (!chatRoom.participants.contains(senderId)) {
            throw ApiException("채팅방에 속해있지 않습니다", ErrorCode.USER_NOT_IN_ROOM)
        }

        // 예약 시간 검증 (현재보다 미래인지)
        if (scheduledAt.isBefore(Instant.now())) {
            throw ApiException("예약 시간은 현재 시간 이후여야 합니다.", ErrorCode.INVALID_SCHEDULED_TIME)
        }

        // 예약 메시지 생성
        val scheduledMessage = ScheduledMessage(
            roomId = roomId.value,
            senderId = senderId.value,
            content = MessageContent(
                text = content,
                type = MessageType.TEXT
            ),
            scheduledAt = scheduledAt
        )

        // 예약 메시지 저장 후 반환
        val saveScheduledMessage = scheduledMessagePort.saveScheduledMessage(scheduledMessage)
        return scheduledMessageMapper.toScheduledMessageResponseDto(saveScheduledMessage)
    }

    override fun cancelScheduledMessage(command: CancelScheduledMessageCommand): ScheduledMessageResponseDto {
        val scheduledMessageId = command.scheduledMessageId
        val userId = command.userId

        // 예약 메시지 존재여부 확인
        val scheduledMessage = (scheduledMessagePort.findById(scheduledMessageId.toObjectId())
            ?: throw ApiException("예약 메시지를 찾을 수 없습니다.", ErrorCode.SCHEDULED_MESSAGE_NOT_FOUND))

        // 본인이 예약한 메시지인지 확인
        if (scheduledMessage.senderId != userId.value) {
            throw ApiException("본인이 예약한 메시지만 취소할 수 있습니다.", ErrorCode.SCHEDULED_MESSAGE_NOT_OWNED)
        }

        // 이미 처리된 메시지인지 확인
        if (scheduledMessage.status != ScheduledMessageStatus.PENDING) {
            throw ApiException(
                "이미 ${scheduledMessage.status} 상태의 메시지입니다.",
                ErrorCode.SCHEDULED_MESSAGE_ALREADY_PROCESSED
            )
        }

        // 상태 업데이트 및 저장
        val updatedMessage = scheduledMessage.copy(status = ScheduledMessageStatus.CANCELED)
        val saveScheduledMessage = scheduledMessagePort.saveScheduledMessage(updatedMessage)
        return scheduledMessageMapper.toScheduledMessageResponseDto(saveScheduledMessage)
    }

    override fun updateScheduledMessage(command: UpdateScheduledMessageCommand): ScheduledMessageResponseDto {
        val scheduledMessageId = command.scheduledMessageId
        val userId = command.userId.value
        val newContent = command.newContent
        val newScheduledAt = command.newScheduledAt

        // 예약 메시지 조회
        val scheduledMessage = (scheduledMessagePort.findById(scheduledMessageId.toObjectId())
            ?: throw ApiException("예약 메시지를 찾을 수 없습니다.", ErrorCode.SCHEDULED_MESSAGE_NOT_FOUND))

        // 본인 확인
        if (scheduledMessage.senderId != userId) {
            throw ApiException("본인이 예약한 메시지만 수정할 수 있습니다.", ErrorCode.SCHEDULED_MESSAGE_NOT_OWNED)
        }

        // 이미 처리된 메시지인지 확인
        if (scheduledMessage.status != ScheduledMessageStatus.PENDING) {
            throw ApiException(
                "이미 ${scheduledMessage.status} 상태의 메시지입니다.",
                ErrorCode.SCHEDULED_MESSAGE_ALREADY_PROCESSED
            )
        }

        // 새 예약 시간 검증 (현재보다 미래인지)
        val scheduledAt = newScheduledAt ?: scheduledMessage.scheduledAt
        if (scheduledAt.isBefore(Instant.now())) {
            throw ApiException("예약 시간은 현재 시간 이후여야 합니다.", ErrorCode.INVALID_SCHEDULED_TIME)
        }

        // 새 내용 적용
        val updatedContent = scheduledMessage.content.copy(text = newContent)

        // 업데이트된 예약 메시지 생성 및 저장
        val updatedMessage = scheduledMessage.copy(
            content = updatedContent,
            scheduledAt = scheduledAt
        )

        // 업데이트된 메시지 저장 후 반환
        val saveScheduledMessage = scheduledMessagePort.saveScheduledMessage(updatedMessage)
        return scheduledMessageMapper.toScheduledMessageResponseDto(saveScheduledMessage)
    }

    override fun getScheduledMessagesByUser(command: GetScheduledMessagesCommand): List<ScheduledMessageResponseDto> {
        val userId = command.userId.value
        val roomId = command.roomId?.value

        val scheduledMessageList = scheduledMessagePort.findByUserId(userId, roomId)
            .filter { it.status == ScheduledMessageStatus.PENDING }

        // 예약 메시지 목록 반환
        return scheduledMessageList.map { scheduledMessageMapper.toScheduledMessageResponseDto(it) }
    }

    override fun sendScheduledMessageNow(command: SendScheduledMessageNowCommand): ScheduledMessageResponseDto {
        val scheduledMessageId = command.scheduledMessageId
        val userId = command.userId.value

        // 예약 메시지 조회
        val scheduledMessage = (scheduledMessagePort.findById(scheduledMessageId.toObjectId())
            ?: throw ApiException("예약 메시지를 찾을 수 없습니다.", ErrorCode.SCHEDULED_MESSAGE_NOT_FOUND))

        // 본인이 예약한 메시지인지 확인
        if (scheduledMessage.senderId != userId) {
            throw ApiException("본인이 예약한 메시지만 전송할 수 있습니다.", ErrorCode.SCHEDULED_MESSAGE_NOT_OWNED)
        }

        // 이미 처리된 메시지인지 확인
        if (scheduledMessage.status != ScheduledMessageStatus.PENDING) {
            throw ApiException(
                "이미 ${scheduledMessage.status} 상태의 메시지입니다.",
                ErrorCode.SCHEDULED_MESSAGE_ALREADY_PROCESSED
            )
        }

        // 메시지 즉시 전송 (기존 SendMessageUseCase 활용)
        try {
            // 메시지 요청 객체 생성
            val chatMessageRequest = createChatMessageRequest(scheduledMessage)

            // 도메인 메시지 객체 생성
            val chatMessage = createChatMessage(scheduledMessage)

            // 메시지 발행 (Redis, Kafka)
            messagePublisherPort.publish(chatMessageRequest, chatMessage)

            // 상태 업데이트 및 저장
            val updatedMessage = scheduledMessage.copy(status = ScheduledMessageStatus.SENT)
            val saveScheduledMessage = scheduledMessagePort.saveScheduledMessage(updatedMessage)
            return scheduledMessageMapper.toScheduledMessageResponseDto(saveScheduledMessage)
        } catch (e: Exception) {
            logger.error(e) { "예약 메시지 즉시 전송 실패: $scheduledMessageId" }
            throw e
        }
    }

    // ChatMessageRequest 객체 생성
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
            roomId = ChatRoomId.from(scheduledMessage.roomId).toChat(),
            senderId = UserId.from(scheduledMessage.senderId),
            content = scheduledMessage.content,
            status = MessageStatus.SENT, // 예약 메시지는 실행 시점에 이미 처리 완료된 상태
            metadata = scheduledMessage.metadata,
            createdAt = Instant.now()
        )
    }

}
