package com.stark.shoot.infrastructure.exception.web

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = KotlinLogging.logger {}

    // SSE 요청에 대한 특별 처리 추가
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        // SSE 요청인 경우 적절한 응답 반환
        if (request.getHeader("Accept") == MediaType.TEXT_EVENT_STREAM_VALUE) {
            logger.warn { "Exception in SSE stream: ${ex.message}" }
            // SSE 요청에 대한 예외는 이미 SseEmitter.onError에서 처리됨
            // HTTP 204 반환하여 SSE 연결 종료 (클라이언트가 자동 재연결)
            return ResponseEntity.noContent().build()
        }

        // 일반 요청은 기존대로 처리
        logger.error(ex) { "Unhandled exception: ${ex.message}" }
        val responseDto = ResponseDto.error<Unit>(ex)
        return ResponseEntity(responseDto, HttpStatus.INTERNAL_SERVER_ERROR)
    }

}