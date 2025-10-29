package com.stark.shoot.infrastructure.util

/**
 * WebSocket 응답 메시지를 생성하는 유틸리티 클래스
 *
 * 여러 서비스에서 반복되는 성공/실패 응답 패턴을 통합하여
 * 코드 중복을 제거하고 일관성을 유지합니다.
 */
object WebSocketResponseBuilder {

    /**
     * 성공 응답 생성
     *
     * @param data 응답 데이터
     * @param message 성공 메시지 (기본값: "Success")
     * @return 성공 응답 맵
     */
    fun <T> success(data: T, message: String = "Success"): Map<String, Any?> = mapOf(
        "success" to true,
        "message" to message,
        "data" to data
    )

    /**
     * 실패 응답 생성
     *
     * @param message 실패 메시지
     * @param data 추가 데이터 (선택적, 기본값: null)
     * @return 실패 응답 맵
     */
    fun error(message: String, data: Any? = null): Map<String, Any?> = mapOf(
        "success" to false,
        "message" to message,
        "data" to data
    )
}
