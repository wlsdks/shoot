package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncResponseDto
import com.stark.shoot.application.port.`in`.message.GetMessageSyncFlowUseCase
import com.stark.shoot.application.port.`in`.message.SendSyncMessagesToUserUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.enumerate.SyncDirection
import com.stark.shoot.infrastructure.util.toObjectId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.bson.types.ObjectId
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

@UseCase
class MessageSyncService(
    private val loadMessagePort: LoadMessagePort,
    private val messagingTemplate: SimpMessagingTemplate
) : SendSyncMessagesToUserUseCase, GetMessageSyncFlowUseCase {

    /**
     * 클라이언트 재연결 시 메시지 동기화
     *
     * @param request SyncRequestDto 객체
     */
    override fun chatMessagesFlow(
        request: SyncRequestDto
    ): Flow<MessageSyncInfoDto> = flow {
        val roomObjectId = request.roomId.toObjectId()

        val messageFlow = when (request.direction) {
            // 초기 로드 시 메시지 동기화
            SyncDirection.INITIAL -> {
                if (request.lastMessageId == null) {
                    loadMessagePort.findByRoomIdFlow(roomObjectId, 50)
                } else {
                    val lastMessageObjectId = ObjectId(request.lastMessageId)
                    loadMessagePort.findByRoomIdAndAfterIdFlow(roomObjectId, lastMessageObjectId, 30)
                }
            }
            // 이전 메시지 동기화
            SyncDirection.BEFORE -> {
                if (request.lastMessageId != null) {
                    val lastMessageObjectId = ObjectId(request.lastMessageId)
                    loadMessagePort.findByRoomIdAndBeforeIdFlow(roomObjectId, lastMessageObjectId, 30)
                } else {
                    emptyFlow()
                }
            }
            // 이후 메시지 동기화
            SyncDirection.AFTER -> {
                if (request.lastMessageId != null) {
                    val lastMessageObjectId = ObjectId(request.lastMessageId)
                    loadMessagePort.findByRoomIdAndAfterIdFlow(roomObjectId, lastMessageObjectId, 30)
                } else {
                    emptyFlow()
                }
            }
        }

        // 메시지를 DTOs로 변환
        messageFlow.collect { message ->
            emit(mapToSyncInfoDto(message))
        }
    }


    /**
     * WebSocket을 통해 메시지를 사용자에게 전송
     *
     * @param request 동기화 요청 정보
     * @param messages 전송할 메시지 목록
     */
    override fun sendMessagesToUser(request: SyncRequestDto, messages: List<MessageSyncInfoDto>) {
        val response = SyncResponseDto(
            roomId = request.roomId,
            userId = request.userId,
            messages = messages,
            timestamp = Instant.now(),
            count = messages.size,
            direction = request.direction
        )

        // 개인 채널로 동기화 응답 전송 (클라이언트는 /user/queue/sync로 구독)
        messagingTemplate.convertAndSendToUser(
            request.userId,
            "/queue/sync",
            response
        )
    }


    /**
     * ChatMessage를 MessageSyncInfoDto로 변환
     *
     * @param message ChatMessage 객체
     * @return MessageSyncInfoDto 객체
     */
    private fun mapToSyncInfoDto(message: ChatMessage): MessageSyncInfoDto {
        return MessageSyncInfoDto(
            id = message.id ?: "",
            tempId = message.metadata["tempId"] as? String,
            timestamp = message.createdAt ?: Instant.now(),
            senderId = message.senderId,
            status = message.status.name,
            content = MessageContentRequest(
                text = message.content.text,
                type = message.content.type.name,
                attachments = listOf(),
                isEdited = message.content.isEdited,
                isDeleted = message.content.isDeleted
            ),
            readBy = message.readBy
        )
    }

}