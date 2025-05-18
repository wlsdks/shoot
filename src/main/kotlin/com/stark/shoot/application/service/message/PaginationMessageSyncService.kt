package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.web.socket.dto.ReactionDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncResponseDto
import com.stark.shoot.application.port.`in`.message.GetPaginationMessageUseCase
import com.stark.shoot.application.port.`in`.message.SendSyncMessagesToUserUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.reaction.ReactionType
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.enumerate.SyncDirection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.bson.types.ObjectId
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

@UseCase
class PaginationMessageSyncService(
    private val loadMessagePort: LoadMessagePort,
    private val messagingTemplate: SimpMessagingTemplate
) : SendSyncMessagesToUserUseCase, GetPaginationMessageUseCase {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val INITIAL_LOAD_LIMIT = 50
        private const val SYNC_LOAD_LIMIT = 30
    }

    /**
     * 웹소켓을 통해 메시지(이전, 이후)를 페이징해서 조회한다.
     *
     * @param request SyncRequestDto 객체
     */
    override fun getChatMessagesFlow(
        request: SyncRequestDto
    ): Flow<MessageSyncInfoDto> = flow {
        val messageFlow = getMessageFlowByDirection(request)

        // 메시지를 DTOs로 변환
        messageFlow
            .catch { e ->
                logger.error(e) { "메시지 동기화 중 오류 발생: roomId=${request.roomId}, userId=${request.userId}" }
                throw e
            }
            .collect { message ->
                emit(mapToSyncInfoDto(message))
            }
    }

    /**
     * 동기화 방향에 따라 적절한 메시지 Flow를 반환합니다.
     *
     * @param request 동기화 요청 정보
     * @return 메시지 Flow
     */
    private fun getMessageFlowByDirection(request: SyncRequestDto): Flow<ChatMessage> {
        val roomObjectId = request.roomId

        // lastMessageId가 null이 아닌 경우에만 ObjectId로 변환
        val lastMessageObjectId = request.lastMessageId?.let { ObjectId(it) }

        return when (request.direction) {
            // 초기 로드 시 메시지 동기화
            SyncDirection.INITIAL -> {
                if (lastMessageObjectId == null) {
                    loadMessagePort.findByRoomIdFlow(roomObjectId, INITIAL_LOAD_LIMIT)
                } else {
                    loadMessagePort.findByRoomIdAndAfterIdFlow(roomObjectId, lastMessageObjectId, SYNC_LOAD_LIMIT)
                }
            }
            // 이전 메시지 동기화
            SyncDirection.BEFORE -> {
                if (lastMessageObjectId != null) {
                    loadMessagePort.findByRoomIdAndBeforeIdFlow(roomObjectId, lastMessageObjectId, SYNC_LOAD_LIMIT)
                } else {
                    emptyFlow()
                }
            }
            // 이후 메시지 동기화
            SyncDirection.AFTER -> {
                if (lastMessageObjectId != null) {
                    loadMessagePort.findByRoomIdAndAfterIdFlow(roomObjectId, lastMessageObjectId, SYNC_LOAD_LIMIT)
                } else {
                    emptyFlow()
                }
            }
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
            request.userId.toString(),
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
        logger.trace { "메시지 변환: messageId=${message.id}, senderId=${message.senderId}" }

        // 도메인 Attachment 객체를 ID 문자열로 변환
        val attachmentIds = message.content.attachments.map { it.id }

        // 리액션 맵을 ReactionDto 리스트로 변환
        // message.reactions는 ChatMessage의 getter를 통해 messageReactions.reactions에 접근
        val reactionDtos = message.reactions.map { (reactionType, userIds) ->
            val reaction = ReactionType.fromCode(reactionType)
            ReactionDto(
                reactionType = reactionType,
                emoji = reaction?.emoji ?: "",
                description = reaction?.description ?: "",
                userIds = userIds.toList(),
                count = userIds.size
            )
        }

        return MessageSyncInfoDto(
            id = message.id ?: "",
            tempId = message.metadata.tempId,
            timestamp = message.createdAt ?: Instant.now(),
            senderId = message.senderId,
            status = message.status.name,
            content = MessageContentRequest(
                text = message.content.text,
                type = message.content.type,
                attachments = attachmentIds,
                isEdited = message.content.isEdited,
                isDeleted = message.content.isDeleted
            ),
            readBy = message.readBy,
            reactions = reactionDtos
        )
    }

}
