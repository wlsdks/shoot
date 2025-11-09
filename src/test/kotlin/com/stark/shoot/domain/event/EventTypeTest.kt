package com.stark.shoot.domain.event

import com.stark.shoot.domain.shared.event.type.EventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("이벤트 타입 enum 테스트")
class EventTypeTest {

    @Test
    @DisplayName("[happy] EventType 은 MESSAGE_CREATED MESSAGE_UPDATED MESSAGE_DELETED 세 가지가 존재한다")
    fun `EventType 은 MESSAGE_CREATED MESSAGE_UPDATED MESSAGE_DELETED 세 가지가 존재한다`() {
        val names = EventType.values().map { it.name }
        assertThat(names).containsExactly("MESSAGE_CREATED", "MESSAGE_UPDATED", "MESSAGE_DELETED")
    }
}
