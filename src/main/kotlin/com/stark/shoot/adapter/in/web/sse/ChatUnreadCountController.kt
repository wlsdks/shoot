package com.stark.shoot.adapter.`in`.web.sse

import com.stark.shoot.application.port.`in`.chatroom.SseEmitterUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "채팅방 목록", description = "사용자의 채팅방 목록 조회 API")
@RestController
@RequestMapping("/api/v1/chatrooms")
class ChatUnreadCountController(
    private val sseEmitterUseCase: SseEmitterUseCase
) {
    private val logger = KotlinLogging.logger {}

    @Operation(
        summary = "사용자의 SSE 연결",
        description = """
            - 사용자의 채팅방 목록에 안읽은 메시지수와 마지막 메시지를 실시간으로 전송합니다.
            - 친구 추가시 상대방의 소셜 목록에 표시합니다.
            - 채팅방을 만들면 상대방의 채팅방 목록에 표시합니다.
        """
    )
    @GetMapping(
        value = ["/updates/{userId}"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun streamUpdates(@PathVariable userId: String): SseEmitter {
        return try {
            sseEmitterUseCase.createEmitter(userId)
        } catch (e: Exception) {
            logger.error(e) { "SSE 연결 실패: $userId - ${e.message}" }
            sendErrorResponse(e, userId)
        }
    }

    /**
     * SSE 연결 도중 예외 발생 시 에러 전용 SSE 이미터 반환
     *
     * @param e 예외
     * @param userId 사용자 ID
     * @return 에러 전용 SSE 이미터
     */
    private fun sendErrorResponse(
        e: Exception,
        userId: String
    ): SseEmitter {
        // SSE 연결 도중 예외 발생 시 로깅 후 새로운 에러 전용 SSE 이미터 반환
        logger.error(e) { "Error creating SSE emitter for user: $userId" }
        val errorEmitter = SseEmitter(3000L) // 짧은 타임아웃
        errorEmitter.send(
            SseEmitter.event()
                .name("error")
                .data("{\"type\":\"connection_error\",\"message\":\"연결 오류가 발생했습니다, 다시 연결하세요.\"}")
        )
        errorEmitter.complete()
        return errorEmitter
    }

}