package com.stark.shoot.application.port.out.message

import com.stark.shoot.domain.chat.message.type.MessageStatus

/**
 * 메시지 상태 알림을 위한 포트 인터페이스
 * 메시지 상태 변경 및 오류 알림 기능을 제공합니다.
 */
interface MessageStatusNotificationPort {
    /**
     * 메시지 상태 변경을 알립니다.
     *
     * @param roomId 채팅방 ID
     * @param tempId 임시 메시지 ID
     * @param status 메시지 상태
     * @param errorMessage 오류 메시지 (선택적)
     */
    fun notifyMessageStatus(
        roomId: Long,
        tempId: String,
        status: MessageStatus,
        errorMessage: String? = null
    )
    
    /**
     * 메시지 처리 중 발생한 오류를 알립니다.
     *
     * @param roomId 채팅방 ID
     * @param throwable 발생한 예외
     */
    fun notifyMessageError(roomId: Long, throwable: Throwable)
}