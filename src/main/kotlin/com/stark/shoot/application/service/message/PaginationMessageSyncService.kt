package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.socket.mapper.MessageSyncMapper
import com.stark.shoot.adapter.`in`.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.socket.dto.SyncRequestDto
import com.stark.shoot.adapter.`in`.socket.dto.SyncResponseDto
import com.stark.shoot.application.port.`in`.message.GetPaginationMessageUseCase
import com.stark.shoot.application.port.`in`.message.SendSyncMessagesToUserUseCase
import com.stark.shoot.application.port.`in`.message.command.GetPaginationMessageCommand
import com.stark.shoot.application.port.`in`.message.command.SendSyncMessagesToUserCommand
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.thread.ThreadQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.SyncDirection
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.infrastructure.annotation.UseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

/**
 * 페이지네이션 방식으로 메시지를 동기화하는 서비스
 */
@UseCase
class PaginationMessageSyncService(
    private val messageQueryPort: MessageQueryPort,
    private val threadQueryPort: ThreadQueryPort,
    private val messagingTemplate: SimpMessagingTemplate,
    private val messageSyncMapper: MessageSyncMapper
) : SendSyncMessagesToUserUseCase, GetPaginationMessageUseCase {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val INITIAL_LOAD_LIMIT = 50
        private const val SYNC_LOAD_LIMIT = 30
    }

    /**
     * 웹소켓을 통해 메시지(이전, 이후)를 페이징해서 조회한다.
     *
     * @param command GetPaginationMessageCommand 객체
     */
    override fun getChatMessagesFlow(
        command: GetPaginationMessageCommand
    ): Flow<MessageSyncInfoDto> = flow {
        val request = command.request
        val messageFlow = getMessageFlowByDirection(request)

        // 메시지를 DTOs로 변환
        messageFlow
            .catch { e ->
                logger.error(e) { "메시지 동기화 중 오류 발생: roomId=${request.roomId}, userId=${request.userId}" }
                throw e
            }
            .collect { message ->
                val replyCount = if (message.threadId == null && message.id != null) {
                    threadQueryPort.countByThreadId(message.id)
                } else {
                    null
                }
                emit(messageSyncMapper.toSyncInfoDto(message, replyCount))
            }
    }

    /**
     * 동기화 방향에 따라 적절한 메시지 Flow를 반환합니다.
     *
     * @param request 동기화 요청 정보
     * @return 메시지 Flow
     */
    private fun getMessageFlowByDirection(
        request: SyncRequestDto
    ): Flow<ChatMessage> {
        val roomObjectId = ChatRoomId.from(request.roomId)

        // lastMessageId가 null이 아닌 경우에만 ObjectId로 변환
        val hasLastId = !request.lastMessageId.isNullOrBlank()
        val lastMessageObjectId = if (hasLastId) {
            MessageId.from(request.lastMessageId!!)
        } else {
            MessageId.from("default-message-id")
        }

        return when (request.direction) {
            // 초기 로드 시 최신 메시지 동기화
            SyncDirection.INITIAL -> {
                if (!hasLastId) {
                    messageQueryPort.findByRoomIdFlow(
                        roomObjectId, INITIAL_LOAD_LIMIT
                    )
                } else {
                    messageQueryPort.findByRoomIdAndAfterIdFlow(
                        roomObjectId, lastMessageObjectId, SYNC_LOAD_LIMIT
                    )
                }
            }
            // 이전 메시지 동기화
            SyncDirection.BEFORE -> {
                messageQueryPort.findByRoomIdAndBeforeIdFlow(
                    roomObjectId, lastMessageObjectId, SYNC_LOAD_LIMIT
                )
            }
            // 이후 메시지 동기화
            SyncDirection.AFTER -> {
                messageQueryPort.findByRoomIdAndAfterIdFlow(
                    roomObjectId, lastMessageObjectId, SYNC_LOAD_LIMIT
                )
            }
        }
    }


    /**
     * WebSocket을 통해 메시지를 사용자에게 전송
     *
     * @param command 동기화 요청 및 전송할 메시지 목록을 담은 커맨드
     */
    override fun sendMessagesToUser(
        command: SendSyncMessagesToUserCommand
    ) {
        val request = command.request
        val messages = command.messages
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

}
