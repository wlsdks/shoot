package com.stark.shoot.adapter.`in`.web.socket.sync

import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfo
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncResponseDto
import com.stark.shoot.application.port.out.message.LoadChatMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class MessageSyncController(
    private val loadChatMessagePort: LoadChatMessagePort,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}

    @Operation(summary = "클라이언트 재연결 시 메시지 동기화")
    @MessageMapping("/sync")
    fun syncMessages(@Payload request: SyncRequestDto) {
        logger.info { "메시지 동기화 요청: roomId=${request.roomId}, lastMessageId=${request.lastMessageId}, userId=${request.userId}" }

        val roomObjectId = request.roomId.toObjectId()
        // 만약 lastTimestamp가 없으면 1시간 전부터 조회 (여기서는 필요에 따라 수정)
        val lastTimestamp = request.lastTimestamp ?: Instant.now().minusSeconds(60 * 60)

        // 클라이언트가 마지막으로 받은 메시지 이후의 모든 메시지를 가져오기 위해 배치로 조회
        val allMessages = mutableListOf<ChatMessage>()
        if (request.lastMessageId != null) {
            var lastId = request.lastMessageId.toObjectId()
            var batch: List<ChatMessage>
            do {
                // 배치 사이즈 50개씩 조회
                batch = loadChatMessagePort.findByRoomIdAndBeforeId(
                    roomObjectId,
                    lastId,
                    50
                )
                if (batch.isNotEmpty()) {
                    allMessages.addAll(batch)
                    // 마지막 배치의 마지막 메시지의 ID를 갱신
                    lastId = batch.last().id?.toObjectId() ?: break
                }
                // 각 반복에서 batch의 크기가 50이면 아직 더 많은 메시지가 있을 수 있으므로, 마지막 메시지의 ID를 기준으로 다음 배치를 조회합니다.
                // 만약 50개 미만의 메시지가 반환되면 더 이상 조회할 메시지가 없다고 판단하고 반복을 종료합니다.
            } while (batch.size == 50)
        } else {
            // 만약 lastMessageId가 없다면 (예: 처음 접속) 전체 또는 제한 없는 최근 메시지 전체 조회
            // 여기서는 제한 없이 조회하기 위해 매우 큰 limit 값을 전달
            allMessages.addAll(loadChatMessagePort.findByRoomId(roomObjectId, 9999))
        }

        // 동기화 응답 데이터 생성
        val response = SyncResponseDto(
            roomId = request.roomId,
            userId = request.userId,
            messages = allMessages.map { message ->
                MessageSyncInfo(
                    id = message.id ?: "",
                    tempId = message.metadata["tempId"] as? String,
                    timestamp = message.createdAt ?: Instant.now(),
                    senderId = message.senderId,
                    status = message.status.name
                )
            },
            timestamp = Instant.now(),
            count = allMessages.size
        )

        // 개인 채널로 동기화 응답 전송 (클라이언트는 /user/queue/sync로 구독)
        messagingTemplate.convertAndSendToUser(
            request.userId,
            "/queue/sync",
            response
        )

        logger.info { "메시지 동기화 완료: roomId=${request.roomId}, userId=${request.userId}, count=${allMessages.size}" }
    }

}
