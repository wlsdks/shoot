package com.stark.shoot.application.port.`in`.message.consume

import com.stark.shoot.domain.event.MessageEvent

/**
 * Use case for consuming message events from Kafka.
 *
 * Returns true when processing succeeds, false otherwise.
 */
interface ConsumeMessageEventUseCase {
    fun consume(event: MessageEvent): Boolean
}
