package com.stark.shoot.domain.common

interface DomainEvent {
    val occurredOn: Long
        get() = System.currentTimeMillis()
}