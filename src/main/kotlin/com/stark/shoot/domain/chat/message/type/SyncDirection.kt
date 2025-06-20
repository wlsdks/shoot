package com.stark.shoot.domain.chat.message.type

/**
 * 메시지 동기화 방향을 나타내는 열거형
 */
enum class SyncDirection {
    /** 이전 메시지 조회 */
    BEFORE,

    /** 이후 메시지 조회 */
    AFTER,

    /** 초기 로드 */
    INITIAL
}