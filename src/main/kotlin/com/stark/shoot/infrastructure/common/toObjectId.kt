package com.stark.shoot.infrastructure.common

import org.bson.types.ObjectId

/**
 * 문자열을 MongoDB ObjectId로 변환하는 확장 함수.
 * 유효하지 않은 ObjectId일 경우 예외를 던집니다.
 */
fun String.toObjectId(): ObjectId {
    return try {
        ObjectId(this)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("유효하지 않은 ObjectId 형식: $this", e)
    }
}
