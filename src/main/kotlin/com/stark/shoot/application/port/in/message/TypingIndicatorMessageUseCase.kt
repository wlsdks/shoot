package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.web.socket.dto.TypingIndicatorMessage

interface TypingIndicatorMessageUseCase {
    fun sendMessage(message: TypingIndicatorMessage)
}