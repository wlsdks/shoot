package com.stark.shoot.adapter.`in`.rest.dto

import java.time.Instant

/**
 * API 응답을 위한 공통 응답 객체
 *
 * @param T 응답 데이터 타입
 */
data class ResponseDto<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val errorCode: String? = null,
    val timestamp: Instant = Instant.now(),
    val code: Int
) {
    companion object {
        /**
         * 성공 응답 생성 (데이터 포함)
         */
        fun <T> success(data: T): ResponseDto<T> {
            return ResponseDto(true, data, null, null, Instant.now(), 200)
        }

        /**
         * 성공 응답 생성 (데이터 포함, 메시지 포함)
         */
        fun <T> success(data: T, message: String): ResponseDto<T> {
            return ResponseDto(true, data, message, null, Instant.now(), 200)
        }

        /**
         * 성공 응답 생성 (데이터 없음)
         */
        fun <T> success(): ResponseDto<T> {
            return ResponseDto(true, null, "요청이 성공적으로 처리되었습니다.", null, Instant.now(), 200)
        }

        /**
         * 실패 응답 생성
         */
        fun <T> fail(message: String, code: Int = 400): ResponseDto<T> {
            return ResponseDto(false, null, message, null, Instant.now(), code)
        }

        /**
         * 예외에서 실패 응답 생성
         */
        fun <T> error(exception: ApiException): ResponseDto<T> {
            return ResponseDto(
                success = false,
                data = null,
                message = exception.message,
                errorCode = exception.errorCode.code,
                timestamp = Instant.now(),
                code = exception.status.value()
            )
        }

        /**
         * 일반 예외에서 실패 응답 생성
         */
        fun <T> error(exception: Exception, code: Int = 500): ResponseDto<T> {
            return ResponseDto(false, null, exception.message, null, Instant.now(), code)
        }
    }

}