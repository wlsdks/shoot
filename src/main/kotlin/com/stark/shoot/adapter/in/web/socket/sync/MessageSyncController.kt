package com.stark.shoot.adapter.`in`.web.socket.sync

import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfo
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncResponseDto
import com.stark.shoot.application.port.out.message.LoadChatMessagePort
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
        logger.info { "메시지 동기화 요청: roomId = ${request.roomId}, lastMessageId = ${request.lastMessageId}, userId = ${request.userId}" }

        val roomObjectId = request.roomId.toObjectId()
        val lastTimestamp = request.lastTimestamp ?: Instant.now().minusSeconds(60 * 60) // 1시간 전까지만 동기화

        // 클라이언트 마지막 메시지 이후 메시지 조회
        val messages = if (request.lastMessageId != null) {
            loadChatMessagePort.findByRoomIdAndBeforeId(
                roomObjectId,
                request.lastMessageId.toObjectId(),
                50 // 최대 50개
            )
        } else {
            // 타임스탬프 기준 조회 로직
            loadChatMessagePort.findByRoomId(roomObjectId, 50)
        }

        // 누락된 메시지 전송
        val response = SyncResponseDto(
            roomId = request.roomId,
            userId = request.userId,
            messages = messages.map { message ->
                MessageSyncInfo(
                    id = message.id ?: "",
                    tempId = message.metadata["tempId"] as? String,
                    timestamp = message.createdAt ?: Instant.now(),
                    senderId = message.senderId,
                    status = message.status.name
                )
            },
            timestamp = Instant.now(),
            count = messages.size
        )

        // 개인 채널로 동기화 데이터 전송
        messagingTemplate.convertAndSendToUser(
            request.userId,
            "/queue/sync",
            response
        )

        logger.info { "메시지 동기화 완료: ${messages.size}개 메시지, 사용자=${request.userId}" }
    }

}