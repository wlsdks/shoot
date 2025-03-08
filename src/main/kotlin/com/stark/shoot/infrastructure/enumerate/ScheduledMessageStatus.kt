package com.stark.shoot.infrastructure.enumerate

/**
 * 예약된 메시지의 상태
 */
enum class ScheduledMessageStatus {
    PENDING,   // 아직 보내기 전
    SENT,      // 이미 보내진 상태
    CANCELED   // 취소된 상태
}