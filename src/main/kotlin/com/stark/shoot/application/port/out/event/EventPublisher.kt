package com.stark.shoot.application.port.out.event

import com.stark.shoot.domain.shared.event.DomainEvent

/**
 * 도메인 이벤트 발행을 위한 아웃바운드 포트
 *
 * 헥사고날 아키텍처에서 애플리케이션 계층이 인프라스트럭처 계층의
 * 이벤트 발행 메커니즘에 의존하지 않도록 추상화를 제공합니다.
 */
interface EventPublishPort {

    /**
     * 단일 도메인 이벤트 발행
     */
    fun publishEvent(event: DomainEvent)

    /**
     * 여러 도메인 이벤트를 일괄 발행
     */
    fun publishEvents(events: List<DomainEvent>) {
        events.forEach { publishEvent(it) }
    }
}

