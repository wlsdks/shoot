package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.web.dto.message.MessageResponseDto
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.chat.message.vo.MessageId

interface GetMessagesUseCase {
    fun getMessages(roomId: ChatRoomId, lastMessageId: MessageId?, limit: Int): List<MessageResponseDto>
}