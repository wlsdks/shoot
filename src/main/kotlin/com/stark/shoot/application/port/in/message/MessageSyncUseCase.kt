package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto

interface MessageSyncUseCase {
    fun chatMessages(request: SyncRequestDto)
}