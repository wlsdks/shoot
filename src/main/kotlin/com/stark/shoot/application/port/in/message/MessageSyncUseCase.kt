package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.adapter.`in`.web.socket.dto.MessageSyncInfoDto
import com.stark.shoot.adapter.`in`.web.socket.dto.SyncRequestDto
import kotlinx.coroutines.flow.Flow

interface MessageSyncUseCase {
    fun chatMessagesFlow(request: SyncRequestDto): Flow<MessageSyncInfoDto>
    fun sendMessagesToUser(request: SyncRequestDto, messages: List<MessageSyncInfoDto>)
}