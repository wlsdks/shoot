package com.stark.shoot.adapter.`in`.rest.dto

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val status: HttpStatus,
    val message: String
) {
    // 공통 에러 (1000번대)
    INVALID_INPUT("E1001", HttpStatus.BAD_REQUEST, "유효하지 않은 입력입니다"),
    ACCESS_DENIED("E1002", HttpStatus.FORBIDDEN, "접근이 거부되었습니다"),
    RESOURCE_NOT_FOUND("E1003", HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
    DUPLICATE_RESOURCE("E1004", HttpStatus.CONFLICT, "이미 존재하는 리소스입니다"),

    // 인증 관련 에러 (2000번대)
    UNAUTHORIZED("E2001", HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    INVALID_TOKEN("E2002", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    TOKEN_EXPIRED("E2003", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),

    // 메시지 관련 에러 (3000번대)
    MESSAGE_TOO_LONG("E3001", HttpStatus.BAD_REQUEST, "메시지가 너무 깁니다"),
    MESSAGE_ALREADY_DELETED("E3002", HttpStatus.BAD_REQUEST, "이미 삭제된 메시지입니다"),
    MESSAGE_EDIT_TIMEOUT("E3003", HttpStatus.BAD_REQUEST, "메시지 수정 시간이 초과되었습니다"),
    INVALID_SCHEDULED_TIME("E3004", HttpStatus.BAD_REQUEST, "예약 시간은 현재 시간 이후여야 합니다"),
    SCHEDULED_MESSAGE_NOT_FOUND("E3005", HttpStatus.NOT_FOUND, "예약된 메시지를 찾을 수 없습니다"),
    SCHEDULED_MESSAGE_NOT_OWNED("E3006", HttpStatus.FORBIDDEN, "예약된 메시지를 소유하고 있지 않습니다"),
    SCHEDULED_MESSAGE_ALREADY_PROCESSED("E3007", HttpStatus.BAD_REQUEST, "이미 처리된 예약 메시지입니다"),

    // 채팅방 관련 에러 (4000번대)
    ROOM_NOT_FOUND("E4001", HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다"),
    USER_NOT_IN_ROOM("E4002", HttpStatus.FORBIDDEN, "사용자가 채팅방에 속해있지 않습니다"),
    TOO_MANY_PINNED_ROOMS("E4003", HttpStatus.BAD_REQUEST, "고정된 채팅방이 너무 많습니다"),
    FAVORITE_LIMIT_EXCEEDED("E4004", HttpStatus.BAD_REQUEST, "즐겨찾기 개수 제한을 초과했습니다"),

    // 친구 관련 에러 (5000번대)
    FRIEND_REQUEST_ALREADY_SENT("E5001", HttpStatus.CONFLICT, "이미 친구 요청을 보냈습니다"),
    FRIEND_REQUEST_NOT_FOUND("E5002", HttpStatus.NOT_FOUND, "친구 요청을 찾을 수 없습니다"),
    ALREADY_FRIENDS("E5003", HttpStatus.CONFLICT, "이미 친구 관계입니다"),
    SELF_FRIEND_REQUEST("E5004", HttpStatus.BAD_REQUEST, "자기 자신에게 친구 요청을 보낼 수 없습니다"),

    // 외부 시스템 연동 에러 (9000번대)
    EXTERNAL_SERVICE_ERROR("E9001", HttpStatus.INTERNAL_SERVER_ERROR, "외부 서비스 오류"),
    DATABASE_ERROR("E9002", HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 오류"),
    LOCK_ACQUIRE_FAILED("E9003", HttpStatus.INTERNAL_SERVER_ERROR, "락 획득에 실패했습니다"),

    ;

    // 에러 코드 문서화를 위한 함수
    companion object {
        fun getErrorCodeMap(): Map<String, Map<String, Any>> {
            return values().associate { errorCode ->
                errorCode.code to mapOf(
                    "message" to errorCode.message,
                    "status" to errorCode.status.value(),
                    "description" to errorCode.name
                )
            }
        }
    }

}
