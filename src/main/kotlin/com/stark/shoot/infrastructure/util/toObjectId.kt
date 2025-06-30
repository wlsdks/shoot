package com.stark.shoot.infrastructure.util

import org.bson.types.ObjectId

/**
 * 문자열을 MongoDB ObjectId로 변환하는 확장 함수.
 * 유효하지 않은 ObjectId일 경우 예외를 던집니다.
 * 특수한 경우 (예: "default-message-id")는 기본 ObjectId를 반환합니다.
 */
fun String.toObjectId(): ObjectId {
    // 특수 케이스 처리
    if (this == "default-message-id") {
        // 유효한 ObjectId 형식의 기본값 반환 (000000000000000000000000)
        return ObjectId("000000000000000000000000")
    }

    return try {
        ObjectId(this)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("유효하지 않은 ObjectId 형식: $this", e)
    }
}
