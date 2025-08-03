package com.stark.shoot.domain.chat.message.type

enum class MessageStatus {
    SENT,       // 전송 및 영속화 완료 (최종 성공 상태)
    FAILED      // 전송 또는 영속화 실패 (오류 상태)

    // SENDING 상태 제거:
    // - 클라이언트에서 메시지 전송 시 즉시 UI에 표시 (낙관적 업데이트)
    // - 서버에서 처리 완료 후 SENT 또는 FAILED로 상태 업데이트
    // - 이렇게 하면 응답성과 일관성을 모두 확보할 수 있음
}
