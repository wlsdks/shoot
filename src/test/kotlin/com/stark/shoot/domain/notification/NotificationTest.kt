package com.stark.shoot.domain.notification

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.exception.NotificationException
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DisplayName("알림 엔티티 테스트")
class NotificationTest {
    
    @Test
    @DisplayName("[happy] 알림을 읽고 삭제 상태로 만들 수 있다")
    fun markReadAndDelete() {
        val n = Notification.create(
            userId = UserId.from(1L),
            title = "t",
            message = "m",
            type = NotificationType.NEW_MESSAGE,
            sourceId = "s",
            sourceType = SourceType.CHAT
        )
        n.markAsRead()
        assertTrue(n.isRead)
        n.markAsDeleted()
        assertTrue(n.isDeleted)
    }

    @Test
    @DisplayName("[bad] 다른 사용자의 알림이면 예외가 발생한다")
    fun validateOwnershipThrows() {
        val n = Notification.create(
            userId = UserId.from(1L),
            title = "t",
            message = "m",
            type = NotificationType.NEW_MESSAGE,
            sourceId = "s",
            sourceType = SourceType.CHAT
        )
        assertFailsWith<NotificationException> { n.validateOwnership(UserId.from(2L)) }
    }

}
