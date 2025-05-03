package com.stark.shoot.adapter.`in`.web.socket.message

import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import com.stark.shoot.application.port.`in`.message.GetPaginationMessageUseCase
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
class GetMessagePaginationStompHandler(
    private val getPaginationMessageUseCase: GetPaginationMessageUseCase,
    private val sendSyncMessagesToUserUseCase: SendSyncMessagesToUserUseCase,
    private val appCoroutineScope: ApplicationCoroutineScope,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = KotlinLogging.logger {}

    @Operation(
        summary = "메시지 조회",
        description = """
            요청 타입에 따라 '초기, 이전, 다음' 메시지를 페이징해서 조회한다.
            - 요청에 담긴 SyncDirection (INITIAL, BEFORE, AFTER)에 따라 메시지를 조회합니다.
            - API 호출 대신 WebSocket을 통해 실시간으로 메시지를 조회합니다.
        """,
    )
    @MessageMapping("/sync")
    fun syncMessages(@Payload request: SyncRequestDto) {
        // Flow 반환 메서드 호출 (메시지 조회)
        val messagesFlow = getPaginationMessageUseCase.getChatMessagesFlow(request)

        // 코루틴 시작
        appCoroutineScope.launch {
            try {
                val messages = messagesFlow.toList() // Flow를 List로 변환
                sendSyncMessagesToUserUseCase.sendMessagesToUser(request, messages) // 메시지 전송
            } catch (e: Exception) {
                logger.error { "동기화 중 에러 발생" + e.message }
                messagingTemplate.convertAndSendToUser(
                    request.userId.toString(),
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