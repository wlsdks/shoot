package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto

interface SendSyncMessagesToUserUseCase {
    fun sendMessagesToUser(request: SyncRequestDto, messages: List<MessageSyncInfoDto>)
}