package com.stark.shoot.infrastructure.enumerate

enum class SyncDirection {
    BEFORE,  // 이전 메시지 조회
    AFTER,   // 이후 메시지 조회
    INITIAL  // 초기 로드 (기본값)
}