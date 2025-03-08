package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncResponseDto
import com.stark.shoot.application.port.`in`.message.MessageSyncUseCase
import com.stark.shoot.application.port.out.message.LoadChatMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.types.ObjectId
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

@UseCase
class MessageSyncService(
    private val loadChatMessagePort: LoadChatMessagePort,
    private val messagingTemplate: SimpMessagingTemplate
) : MessageSyncUseCase {
    private val logger = KotlinLogging.logger {}


    /**
     * 클라이언트 재연결 시 메시지 동기화
     *
     * @param request SyncRequestDto 객체
     */
    override fun chatMessages(
        request: SyncRequestDto
    ) {
        val roomObjectId = request.roomId.toObjectId()
        val allMessages = mutableListOf<ChatMessage>()

        // lastMessageId가 없는 경우 (예: 처음 접속) 전체 또는 제한 없는 최근 메시지 전체 조회 (제한 없이 조회하기 위해 매우 큰 limit 값을 전달)
        if (request.lastMessageId == null) {
            allMessages.addAll(loadChatMessagePort.findByRoomId(roomObjectId, 9999))
        }

        // lastMessageId가 있으면 클라이언트가 마지막으로 받은 메시지 이후의 모든 메시지를 가져오기 위해 배치로 조회
        if (request.lastMessageId != null) {
            loadMessagesAfterLastMessage(request.lastMessageId, roomObjectId, allMessages)
        }

        // 동기화 응답 데이터 생성
        val response = createSyncResponseDto(request, allMessages)

        // 개인 채널로 동기화 응답 전송 (클라이언트는 /user/queue/sync로 구독)
        messagingTemplate.convertAndSendToUser(
            request.userId,
            "/queue/sync",
            response
        )
    }


    /**
     * 마지막 메시지 이후의 모든 메시지를 가져오기 위해 배치로 조회
     *
     * @param lastMessageId 마지막 메시지 ID
     * @param roomObjectId 채팅방 ID
     * @param allMessages 모든 메시지를 저장할 목록
     */
    private fun loadMessagesAfterLastMessage(
        lastMessageId: String,
        roomObjectId: ObjectId,
        allMessages: MutableList<ChatMessage>
    ) {
        var lastId = lastMessageId.toObjectId()
        var batch: List<ChatMessage>

        do {
            // 배치 사이즈 50개씩 조회
            batch = loadChatMessagePort.findByRoomIdAndBeforeId(
                roomObjectId,
                lastId,
                50
            )
            // 조회된 메시지가 있으면 allMessages에 추가
            if (batch.isNotEmpty()) {
                allMessages.addAll(batch)
                // 마지막 배치의 마지막 메시지의 ID를 갱신
                lastId = batch.last().id?.toObjectId() ?: break
            }
            // 각 반복에서 batch의 크기가 50이면 아직 더 많은 메시지가 있을 수 있으므로, 마지막 메시지의 ID를 기준으로 다음 배치를 조회합니다.
            // 만약 50개 미만의 메시지가 반환되면 더 이상 조회할 메시지가 없다고 판단하고 반복을 종료합니다.
        } while (batch.size == 50)
    }


    /**
     * 동기화 응답 데이터 생성
     *
     * @param request SyncRequestDto 객체
     * @param allMessages 모든 메시지 목록
     * @return 완성된 동기화 응답 데이터
     */
    private fun createSyncResponseDto(
        request: SyncRequestDto,
        allMessages: MutableList<ChatMessage>
    ): SyncResponseDto {
        val response = SyncResponseDto(
            roomId = request.roomId,
            userId = request.userId,
            messages = allMessages.map { message ->
                MessageSyncInfoDto(
                    id = message.id ?: "",
                    tempId = message.metadata["tempId"] as? String,
                    timestamp = message.createdAt ?: Instant.now(),
                    senderId = message.senderId,
                    status = message.status.name,
                    content = MessageContentRequest(  // 추가
                        text = message.content.text,
                        type = message.content.type.name,
                        attachments = listOf(),
                        isEdited = message.content.isEdited,
                        isDeleted = message.content.isDeleted
                    ),
                    readBy = message.readBy  // 추가
                )
            },
            timestamp = Instant.now(),
            count = allMessages.size
        )
        return response
    }

}