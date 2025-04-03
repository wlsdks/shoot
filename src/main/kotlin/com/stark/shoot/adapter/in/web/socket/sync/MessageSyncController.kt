package com.stark.shoot.adapter.`in`.web.socket.sync

import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.application.port.`in`.message.GetMessageSyncFlowUseCase
import com.stark.shoot.application.port.`in`.message.SendSyncMessagesToUserUseCase
import com.stark.shoot.infrastructure.config.async.ApplicationCoroutineScope
import com.stark.shoot.infrastructure.exception.web.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.RestController

@RestController
class MessageSyncController(
    private val getMessageSyncFlowUseCase: GetMessageSyncFlowUseCase,
    private val sendSyncMessagesToUserUseCase: SendSyncMessagesToUserUseCase,
    private val appCoroutineScope: ApplicationCoroutineScope,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}

    @Operation(summary = "클라이언트 재연결 시 메시지 동기화")
    @MessageMapping("/sync")
    fun syncMessages(@Payload request: SyncRequestDto) {
        // Flow 반환 메서드 호출
        val messagesFlow = getMessageSyncFlowUseCase.chatMessagesFlow(request)

        // 코루틴 시작
        appCoroutineScope.launch {
            try {
                val messages = messagesFlow.toList() // Flow를 List로 변환
                sendSyncMessagesToUserUseCase.sendMessagesToUser(request, messages) // 메시지 전송
            } catch (e: Exception) {
                logger.error { "동기화 중 에러 발생" + e.message }
                messagingTemplate.convertAndSendToUser(
                    request.userId,
                    "/queue/errors",
                    ErrorResponse(
                        status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        message = "메시지 동기화 중 오류가 발생했습니다.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

}
