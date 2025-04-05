package com.stark.shoot.adapter.out.persistence.postgres.entity.enumerate

enum class UserStatus {
    OFFLINE,        // 오프라인
    ONLINE,         // 온라인
    BUSY,           // 바쁨
    AWAY,           // 자리 비움
    INVISIBLE,      // 보이지 않음 (온라인 상태 숨김)
    DO_NOT_DISTURB, // 방해 금지
    IDLE            // 대기 중 (활동 없음)
}