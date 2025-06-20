package com.stark.shoot.application.port.out.event

import com.stark.shoot.domain.event.DomainEvent

interface EventPublisher {
    fun publish(event: DomainEvent)
}