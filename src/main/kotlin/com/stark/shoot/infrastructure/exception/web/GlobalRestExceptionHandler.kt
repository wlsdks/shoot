package com.stark.shoot.domain.exception.web

import com.stark.shoot.adapter.`in`.rest.dto.ApiException
import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto
import com.stark.shoot.adapter.`in`.rest.dto.ResponseDto.Companion.error
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class GlobalRestExceptionHandler {

    val logger = KotlinLogging.logger {}

    /**
     * Spring MVC 관련 예외 - 지원되지 않는 HTTP 메서드
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException
    ): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to 405,
            "message" to "지원되지 않는 메서드: ${ex.method}",
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity(response, HttpStatus.METHOD_NOT_ALLOWED)
    }

    /**
     * Spring MVC 관련 예외 - 요청 본문 파싱 오류
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException
    ): ResponseEntity<ResponseDto<Any>> {
        logger.error(ex) { "요청 본문 파싱 오류: ${ex.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ResponseDto.fail("잘못된 요청 형식입니다", 400))
    }

    /**
     * AOP에서 변환한 ApiException 처리 (단일 핸들러로 통합)
     */
    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        ex: ApiException,
        request: WebRequest?
    ): ResponseEntity<ResponseDto<Any>> {
        // ApiException에는 이미 적절한 메시지와 에러 코드가 설정되어 있으므로
        // 여기서는 간단히 상태 코드와 함께 응답만 반환합니다
        return ResponseEntity
            .status(ex.status)
            .body(error(ex))
    }

    /**
     * 기타 예외 처리 (AOP가 처리하지 못한 예외)
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception
    ): ResponseEntity<ResponseDto<Any>> {
        logger.error(ex) { "서버 오류: ${ex.message}" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error(ex, 500))
    }

}