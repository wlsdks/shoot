package com.stark.shoot.application.port.out

import com.stark.shoot.domain.common.DomainEvent

interface EventPublisher {
    fun publish(event: DomainEvent)
}