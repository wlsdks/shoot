package com.stark.shoot.domain.chat.pin

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.pin.vo.MessagePinId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("메시지 고정 Aggregate 테스트")
class MessagePinTest {

    @Test
    @DisplayName("메시지 고정 생성 - 팩토리 메서드")
    fun `create pin using factory method`() {
        // given
        val messageId = MessageId.from("msg-1")
        val roomId = ChatRoomId.from(1L)
        val userId = UserId.from(100L)

        // when
        val pin = MessagePin.create(messageId, roomId, userId)

        // then
        assertThat(pin.messageId).isEqualTo(messageId)
        assertThat(pin.roomId).isEqualTo(roomId)
        assertThat(pin.pinnedBy).isEqualTo(userId)
        assertThat(pin.id).isNull() // 새 엔티티는 ID가 null
        assertThat(pin.pinnedAt).isNotNull()
        assertThat(pin.createdAt).isNotNull()
    }

    @Test
    @DisplayName("메시지 고정 생성 - 생성자 직접 호출")
    fun `create pin using constructor`() {
        // given
        val messageId = MessageId.from("msg-2")
        val roomId = ChatRoomId.from(2L)
        val userId = UserId.from(200L)
        val now = Instant.now()

        // when
        val pin = MessagePin(
            id = MessagePinId.from(1L),
            messageId = messageId,
            roomId = roomId,
            pinnedBy = userId,
            pinnedAt = now,
            createdAt = now
        )

        // then
        assertThat(pin.id?.value).isEqualTo(1L)
        assertThat(pin.messageId).isEqualTo(messageId)
        assertThat(pin.roomId).isEqualTo(roomId)
        assertThat(pin.pinnedBy).isEqualTo(userId)
        assertThat(pin.pinnedAt).isEqualTo(now)
        assertThat(pin.createdAt).isEqualTo(now)
    }

    @Test
    @DisplayName("메시지 고정 - ID 참조 패턴 확인")
    fun `pin uses id references not object references`() {
        // given
        val messageId = MessageId.from("msg-3")
        val roomId = ChatRoomId.from(3L)
        val userId = UserId.from(300L)

        // when
        val pin = MessagePin.create(messageId, roomId, userId)

        // then
        // MessagePin은 다른 Aggregate를 ID로만 참조해야 함
        assertThat(pin.messageId).isInstanceOf(MessageId::class.java)
        assertThat(pin.roomId).isInstanceOf(ChatRoomId::class.java)
        assertThat(pin.pinnedBy).isInstanceOf(UserId::class.java)
    }

    @Test
    @DisplayName("메시지 고정 - 불변성 확인")
    fun `pin is immutable`() {
        // given
        val messageId = MessageId.from("msg-4")
        val roomId = ChatRoomId.from(4L)
        val userId = UserId.from(400L)
        val pin = MessagePin.create(messageId, roomId, userId)

        // when
        val copy = pin.copy(pinnedBy = UserId.from(500L))

        // then
        // 원본은 변경되지 않음
        assertThat(pin.pinnedBy).isEqualTo(userId)
        assertThat(copy.pinnedBy).isEqualTo(UserId.from(500L))
    }

    @Test
    @DisplayName("메시지 고정 ID - Value Object 동등성")
    fun `pin id value object equality`() {
        // given
        val id1 = MessagePinId.from(1L)
        val id2 = MessagePinId.from(1L)
        val id3 = MessagePinId.from(2L)

        // then
        assertThat(id1).isEqualTo(id2)
        assertThat(id1).isNotEqualTo(id3)
        assertThat(id1.value).isEqualTo(1L)
    }

    @Test
    @DisplayName("메시지 고정 - 시간 정보 자동 설정")
    fun `pin timestamps are set automatically`() {
        // given
        val before = Instant.now().minusSeconds(1)

        // when
        val pin = MessagePin.create(
            MessageId.from("msg-5"),
            ChatRoomId.from(5L),
            UserId.from(500L)
        )

        // then
        val after = Instant.now().plusSeconds(1)
        assertThat(pin.pinnedAt).isBetween(before, after)
        assertThat(pin.createdAt).isBetween(before, after)
    }
}
