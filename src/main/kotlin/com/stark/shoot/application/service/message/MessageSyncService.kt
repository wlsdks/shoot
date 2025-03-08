package com.stark.shoot.application.service.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageContentRequest
import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncResponseDto
import com.stark.shoot.application.port.`in`.message.MessageSyncUseCase
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.enumerate.SyncDirection
import com.stark.shoot.infrastructure.util.toObjectId
import org.bson.types.ObjectId
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

@UseCase
class MessageSyncService(
    private val loadMessagePort: LoadMessagePort,
    private val messagingTemplate: SimpMessagingTemplate
) : MessageSyncUseCase {

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

        when (request.direction) {
            SyncDirection.BEFORE -> {
                // 이전 메시지 조회
                if (request.lastMessageId != null) {
                    loadMessagesBeforeLastMessage(request.lastMessageId, roomObjectId, allMessages)
                }
            }

            SyncDirection.AFTER -> {
                // 이후 메시지 조회 (lastMessageId보다 이후 메시지)
                if (request.lastMessageId != null) {
                    loadMessagesAfterLastMessage(request.lastMessageId, roomObjectId, allMessages)
                }
            }

            SyncDirection.INITIAL -> {
                // 기본 동작: lastMessageId가 없으면 전체 조회, 있으면 이후 메시지 조회
                if (request.lastMessageId == null) {
                    allMessages.addAll(loadMessagePort.findByRoomId(roomObjectId, 50))
                } else {
                    loadMessagesAfterLastMessage(request.lastMessageId, roomObjectId, allMessages)
                }
            }
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
     * 지정된 메시지 이전의 메시지를 가져오기 위해 배치로 조회
     *
     * @param messageId 기준 메시지 ID
     * @param roomObjectId 채팅방 ID
     * @param allMessages 모든 메시지를 저장할 목록
     */
    private fun loadMessagesBeforeLastMessage(
        messageId: String,
        roomObjectId: ObjectId,
        allMessages: MutableList<ChatMessage>
    ) {
        val lastId = messageId.toObjectId()

        // 이전 메시지 조회 (단일 배치)
        val batch = loadMessagePort.findByRoomIdAndBeforeId(
            roomObjectId,
            lastId,
            30  // 이전 메시지는 한 번에 30개만 조회
        )

        // 조회된 메시지가 있으면 allMessages에 추가
        if (batch.isNotEmpty()) {
            allMessages.addAll(batch)
        }
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
            batch = loadMessagePort.findByRoomIdAndAfterId(
                roomObjectId,
                lastId,
                50  // 이후 메시지는 한 번에 50개까지 조회
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
                    content = MessageContentRequest(
                        text = message.content.text,
                        type = message.content.type.name,
                        attachments = listOf(),
                        isEdited = message.content.isEdited,
                        isDeleted = message.content.isDeleted
                    ),
                    readBy = message.readBy
                )
            },
            timestamp = Instant.now(),
            count = allMessages.size,
            direction = request.direction
        )
        return response
    }

}