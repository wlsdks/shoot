package com.stark.shoot.infrastructure.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalRestExceptionHandler {

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
    ): ResponseEntity<Map<String, Any>> {
        val response = mapOf(
            "status" to 500,
            "message" to "서버 내부 오류: ${ex.message}",
            "timestamp" to System.currentTimeMillis()
        )
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR)
    }

}