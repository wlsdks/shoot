package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.shared.event.MessageEvent

interface HandleMessageEventUseCase {
    fun handle(event: MessageEvent): Boolean
}
