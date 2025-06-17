package com.stark.shoot.domain.common

interface DomainEvent {
    /**
     * 이벤트가 발생한 시각(밀리초)
     */
    val occurredOn: Long
}