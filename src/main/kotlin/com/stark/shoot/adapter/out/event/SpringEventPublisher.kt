package com.stark.shoot.adapter.out.event

import com.stark.shoot.application.port.out.event.EventPublishPort
import com.stark.shoot.domain.event.DomainEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * SpringEventPublisher는 출력 포트(Outbound Port)인 EventPublishPort를 구현하는 아웃바운드 어댑터입니다.
 * 도메인 이벤트를 스프링의 이벤트 시스템을 통해 발행합니다.
 */
@Component
class SpringEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : EventPublishPort {

    override fun publishEvent(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }

}