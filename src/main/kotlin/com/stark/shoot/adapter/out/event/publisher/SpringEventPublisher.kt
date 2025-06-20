package com.stark.shoot.adapter.out.event.publisher

import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.domain.event.DomainEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * SpringEventPublisher는 출력 포트(Outbound Port)인 EventPublisher를 구현하는 아웃바운드 어댑터(Outbound Adapter)입니다.
 * 도메인 이벤트를 스프링의 이벤트 시스템을 통해 발행합니다. (애플리케이션 코어에서 외부로 이벤트를 발행하는 아웃바운드 어댑터.)
 */
@Component
class SpringEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : EventPublisher {

    override fun publish(event: DomainEvent) {
        applicationEventPublisher.publishEvent(event)
    }

}