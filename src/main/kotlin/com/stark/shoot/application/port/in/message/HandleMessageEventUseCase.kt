package com.stark.shoot.application.port.`in`.message

import com.stark.shoot.domain.event.MessageEvent

/**
 * Use case for handling incoming message events.
 *
 * Returns true when processing succeeds, false otherwise.
 */
interface HandleMessageEventUseCase {
    fun handle(event: MessageEvent): Boolean
}
