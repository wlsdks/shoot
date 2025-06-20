package com.stark.shoot.domain.chat.message

import com.stark.shoot.domain.chat.message.type.MessageStatus
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.type.ScheduledMessageStatus
import com.stark.shoot.domain.chat.message.type.SyncDirection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("메시지 관련 enum 테스트")
class MessageEnumsTest {

    @Test
    fun `MessageType 은 TEXT FILE URL EMOTICON 4가지를 가진다`() {
        val values = MessageType.values()
        assertThat(values.map { it.name }).containsExactlyInAnyOrder("TEXT", "FILE", "URL", "EMOTICON")
    }

    @Test
    fun `MessageStatus 값 확인`() {
        val values = MessageStatus.values()
        assertThat(values.map { it.name }).containsExactly(
            "SENDING",
            "PROCESSING",
            "SENT_TO_KAFKA",
            "SAVED",
            "FAILED"
        )
    }

    @Test
    fun `ScheduledMessageStatus 값 확인`() {
        val values = ScheduledMessageStatus.values()
        assertThat(values.map { it.name }).containsExactly("PENDING", "SENT", "CANCELED")
    }

    @Test
    fun `SyncDirection 값 확인`() {
        val values = SyncDirection.values()
        assertThat(values.map { it.name }).containsExactly("BEFORE", "AFTER", "INITIAL")
    }
}
