package com.stark.shoot.application.port.out.message

import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest
import com.stark.shoot.domain.chat.message.ChatMessage

/**
 * 메시지 발행을 위한 포트 인터페이스
 * 메시지를 발행하고 처리 오류를 처리하는 기능을 제공합니다.
 */
interface MessagePublisherPort {
    /**
     * 메시지를 발행합니다.
     *
     * @param request 메시지 요청 DTO
     * @param domainMessage 도메인 메시지
     */
    fun publish(request: ChatMessageRequest, domainMessage: ChatMessage)

    /**
     * 메시지 처리 중 발생한 오류를 처리합니다.
     *
     * @param request 메시지 요청 DTO
     * @param throwable 발생한 예외
     */
    fun handleProcessingError(request: ChatMessageRequest, throwable: Throwable)
}