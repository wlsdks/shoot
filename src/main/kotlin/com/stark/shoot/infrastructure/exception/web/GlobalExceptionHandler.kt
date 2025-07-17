package com.stark.shoot.infrastructure.exception.web

import com.stark.shoot.adapter.`in`.web.dto.ResponseDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        // 일반 요청은 기존대로 처리
        logger.error(ex) { "Unhandled exception: ${ex.message}" }
        val responseDto = ResponseDto.error<Unit>(ex)
        return ResponseEntity(responseDto, HttpStatus.INTERNAL_SERVER_ERROR)
    }

}