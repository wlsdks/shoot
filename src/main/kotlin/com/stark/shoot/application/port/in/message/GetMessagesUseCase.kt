package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto

interface GetMessagesUseCase {
    fun getMessages(roomId: String, lastId: String?, limit: Int): List<MessageResponseDto>
}