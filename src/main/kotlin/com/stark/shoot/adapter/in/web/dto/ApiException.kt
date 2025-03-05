package com.stark.shoot.adapter.`in`.web.dto

import org.springframework.http.HttpStatus

/**
 * API 요청 처리 중 발생하는 모든 예외를 표현하는 단일 예외 클래스
 *
 * @param message 예외 메시지
 * @param errorCode 에러 코드
 * @param status HTTP 상태 코드
 * @param cause 원인 예외
 */
class ApiException(
    override val message: String,
    val errorCode: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    companion object {
        // 에러 코드 상수 정의

        // 공통 에러 (1000번대)
        const val INVALID_INPUT = "E1001"
        const val ACCESS_DENIED = "E1002"
        const val RESOURCE_NOT_FOUND = "E1003"
        const val DUPLICATE_RESOURCE = "E1004"

        // 인증 관련 에러 (2000번대)
        const val UNAUTHORIZED = "E2001"
        const val INVALID_TOKEN = "E2002"
        const val TOKEN_EXPIRED = "E2003"

        // 메시지 관련 에러 (3000번대)
        const val MESSAGE_TOO_LONG = "E3001"
        const val MESSAGE_ALREADY_DELETED = "E3002"
        const val MESSAGE_EDIT_TIMEOUT = "E3003"

        // 채팅방 관련 에러 (4000번대)
        const val ROOM_NOT_FOUND = "E4001"
        const val USER_NOT_IN_ROOM = "E4002"
        const val TOO_MANY_PINNED_ROOMS = "E4003"

        // 친구 관련 에러 (5000번대)
        const val FRIEND_REQUEST_ALREADY_SENT = "E5001"
        const val FRIEND_REQUEST_NOT_FOUND = "E5002"
        const val ALREADY_FRIENDS = "E5003"
        const val SELF_FRIEND_REQUEST = "E5004"

        // 외부 시스템 연동 에러 (9000번대)
        const val EXTERNAL_SERVICE_ERROR = "E9001"
        const val DATABASE_ERROR = "E9002"
    }

}