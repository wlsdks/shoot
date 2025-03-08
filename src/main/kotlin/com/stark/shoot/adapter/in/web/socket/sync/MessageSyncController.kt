package com.stark.shoot.adapter.`in`.web.socket.sync

import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.application.port.`in`.message.MessageSyncUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.web.bind.annotation.RestController

@RestController
class MessageSyncController(
    private val messageSyncUseCase: MessageSyncUseCase,
) {
    private val logger = KotlinLogging.logger {}

    @Operation(summary = "클라이언트 재연결 시 메시지 동기화")
    @MessageMapping("/sync")
    fun syncMessages(@Payload request: SyncRequestDto) {
        messageSyncUseCase.chatMessages(request)
        logger.info { "메시지 동기화 요청 처리 완료: userId=${request.userId}, roomId=${request.roomId}" }
    }

}
