package com.stark.shoot.infrastructure.exception.web

import com.stark.shoot.adapter.`in`.web.dto.ApiException
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import com.stark.shoot.adapter.`in`.web.dto.ResponseDto.Companion.error
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest


@RestControllerAdvice
class GlobalRestExceptionHandler {

    val logger = KotlinLogging.logger {}

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

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception
    ): ResponseEntity<ResponseDto<Any>> {
        logger.error(ex) { "서버 오류: " + ex.message }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error(ex, 500))
    }

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        ex: ApiException,
        request: WebRequest?
    ): ResponseEntity<ResponseDto<Any>> {
        logger.error(ex) { "API 예외: " + ex.message }

        return ResponseEntity
            .status(ex.status)
            .body(error(ex))
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(
        ex: ResourceNotFoundException, request: WebRequest?
    ): ResponseEntity<ResponseDto<Any>> {
        logger.error(ex) { "리소스 찾을 수 없음: " + ex.message }

        val apiException = ApiException(
            ex.message!!,
            ApiException.RESOURCE_NOT_FOUND,
            HttpStatus.NOT_FOUND,
            ex
        )

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error(apiException))
    }

    @ExceptionHandler(InvalidInputException::class)
    fun handleInvalidInputException(
        ex: InvalidInputException, request: WebRequest?
    ): ResponseEntity<ResponseDto<Any>> {
        logger.error(ex) { "잘못된 입력: " + ex.message }

        val apiException = ApiException(
            ex.message!!,
            ApiException.INVALID_INPUT,
            HttpStatus.BAD_REQUEST,
            ex
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error(apiException))
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(
        ex: UnauthorizedException, request: WebRequest?
    ): ResponseEntity<ResponseDto<Any>> {
        logger.error(ex) { "인증 실패: " + ex.message }

        val apiException = ApiException(
            ex.message!!,
            ApiException.UNAUTHORIZED,
            HttpStatus.UNAUTHORIZED,
            ex
        )

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(error(apiException))
    }

    @ExceptionHandler(JwtAuthenticationException::class)
    fun handleJwtAuthenticationException(
        ex: JwtAuthenticationException, request: WebRequest?
    ): ResponseEntity<ResponseDto<Any>> {
        logger.error(ex) { "JWT 인증 실패: " + ex.message }

        val apiException = ApiException(
            ex.message!!,
            ApiException.INVALID_TOKEN,
            HttpStatus.UNAUTHORIZED,
            ex
        )

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(error(apiException))
    }

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshTokenException(
        ex: InvalidRefreshTokenException, request: WebRequest?
    ): ResponseEntity<ResponseDto<Any>> {
        logger.error(ex) { "리프레시 토큰 오류: " + ex.message }

        val apiException = ApiException(
            ex.message!!,
            ApiException.TOKEN_EXPIRED,
            HttpStatus.UNAUTHORIZED,
            ex
        )

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(error(apiException))
    }

}