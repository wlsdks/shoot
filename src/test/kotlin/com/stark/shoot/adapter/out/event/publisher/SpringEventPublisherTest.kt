package com.stark.shoot.adapter.out.event.publisher

import com.stark.shoot.domain.event.DomainEvent
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher

@DisplayName("스프링 이벤트 발행자 테스트")
class SpringEventPublisherTest {

    private val applicationEventPublisher = mock(ApplicationEventPublisher::class.java)
    private val eventPublisher = SpringEventPublisher(applicationEventPublisher)

    // 테스트용 도메인 이벤트 구현
    private class TestDomainEvent(override val occurredOn: Long = System.currentTimeMillis()) : DomainEvent

    @Test
    @DisplayName("[happy] 도메인 이벤트를 발행할 수 있다")
    fun `도메인 이벤트를 발행할 수 있다`() {
        // given
        val event = TestDomainEvent()

        // when
        eventPublisher.publish(event)

        // then
        verify(applicationEventPublisher).publishEvent(event)
    }

    @Test
    @DisplayName("[happy] 여러 도메인 이벤트를 발행할 수 있다")
    fun `여러 도메인 이벤트를 발행할 수 있다`() {
        // given
        val events = listOf(
            TestDomainEvent(),
            TestDomainEvent(),
            TestDomainEvent()
        )

        // when
        events.forEach { event ->
            eventPublisher.publish(event)
        }

        // then
        events.forEach { event ->
            verify(applicationEventPublisher).publishEvent(event)
        }
    }
}
