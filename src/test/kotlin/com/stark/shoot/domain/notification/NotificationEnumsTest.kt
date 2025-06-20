package com.stark.shoot.domain.notification

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("알림 관련 enum 테스트")
class NotificationEnumsTest {

    @Test
    fun `NotificationType enum 모든 값 존재 확인`() {
        val names = NotificationType.values().map { it.name }
        assertThat(names).contains(
            "NEW_MESSAGE",
            "MENTION",
            "REACTION",
            "PIN",
            "FRIEND_REQUEST",
            "FRIEND_ACCEPTED",
            "FRIEND_REJECTED",
            "FRIEND_REMOVED",
            "SYSTEM_ANNOUNCEMENT",
            "SYSTEM_MAINTENANCE",
            "OTHER"
        )
    }

    @Test
    fun `SourceType enum 모든 값 존재 확인`() {
        val names = SourceType.values().map { it.name }
        assertThat(names).contains(
            "CHAT",
            "CHAT_ROOM",
            "USER",
            "FRIEND",
            "SYSTEM",
            "OTHER"
        )
    }
}
