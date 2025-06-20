package com.stark.shoot.application.service.message.schedule

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ErrorCode
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.web.dto.message.schedule.ScheduledMessageResponseDto
import com.stark.shoot.adapter.`in`.web.dto.message.toRequestDto
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ScheduledMessageMapper
import com.stark.shoot.application.port.`in`.message.schedule.ScheduledMessageUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.message.ScheduledMessagePort
import com.stark.shoot.domain.chat.message.vo.MessageContent
import com.stark.shoot.domain.chat.message.ScheduledMessage
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

@UseCase
class ScheduledMessageService(
    private val scheduledMessagePort: ScheduledMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val scheduledMessageMapper: ScheduledMessageMapper,
) : ScheduledMessageUseCase {

    private val logger = KotlinLogging.logger {}

    override fun scheduleMessage(
        roomId: ChatRoomId,
        senderId: UserId,
        content: String,
        scheduledAt: Instant
    ): ScheduledMessageResponseDto {
        // 채팅방 존재여부 확인
        val chatRoom = (loadChatRoomPort.findById(roomId)
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

    override fun cancelScheduledMessage(
        scheduledMessageId: String,
        userId: UserId
    ): ScheduledMessageResponseDto {
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

    override fun updateScheduledMessage(
        scheduledMessageId: String,
        userId: Long,
        newContent: String,
        newScheduledAt: Instant?
    ): ScheduledMessageResponseDto {
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

    override fun getScheduledMessagesByUser(
        userId: Long,
        roomId: Long?
    ): List<ScheduledMessageResponseDto> {
        val scheduledMessageList = scheduledMessagePort.findByUserId(userId, roomId)
            .filter { it.status == ScheduledMessageStatus.PENDING }

        // 예약 메시지 목록 반환
        return scheduledMessageList.map { scheduledMessageMapper.toScheduledMessageResponseDto(it) }
    }

    override fun sendScheduledMessageNow(
        scheduledMessageId: String,
        userId: Long
    ): ScheduledMessageResponseDto {
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
            // 여기서는 예제로 웹소켓이 아닌 방식으로 메시지를 전송합니다.
            // 실제 구현에서는 적절한 채널을 통해 메시지를 전송해야 합니다.
            val chatMessageRequest = createChatMessageRequest(scheduledMessage)

            // todo: 여기서 메시지 저장후 웹소켓으로 전송하도록 해야함

            // 상태 업데이트 및 저장
            val updatedMessage = scheduledMessage.copy(status = ScheduledMessageStatus.SENT)
            val saveScheduledMessage = scheduledMessagePort.saveScheduledMessage(updatedMessage)
            return scheduledMessageMapper.toScheduledMessageResponseDto(saveScheduledMessage)
        } catch (e: Exception) {
            logger.error(e) { "예약 메시지 즉시 전송 실패: $scheduledMessageId" }
            throw e
        }
    }

    // ChatMessageRequest 객체 생성 (실제 구현시 적절하게 변환 필요)
    private fun createChatMessageRequest(scheduledMessage: ScheduledMessage): ChatMessageRequest {
        // 필요한 매핑 로직 구현
        // 예시 코드이므로 실제 구현시 적절하게 변환해야 함
        return ChatMessageRequest(
            roomId = scheduledMessage.roomId,
            senderId = scheduledMessage.senderId,
            content = MessageContentRequest(
                text = scheduledMessage.content.text,
                type = scheduledMessage.content.type
            ),
            metadata = scheduledMessage.metadata.toRequestDto()
        )
    }

}