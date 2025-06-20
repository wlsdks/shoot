package com.stark.shoot.domain.chat.message.type

/**
 * 예약된 메시지의 상태를 나타내는 열거형
 */
enum class ScheduledMessageStatus {
    /** 아직 보내기 전 */
    PENDING,

    /** 이미 보내진 상태 */
    SENT,

    /** 취소된 상태 */
    CANCELED
}