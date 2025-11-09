package com.stark.shoot.domain.chat.readreceipt

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.readreceipt.vo.MessageReadReceiptId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("메시지 읽음 표시 Aggregate 테스트")
class MessageReadReceiptTest {

    @Test
    @DisplayName("읽음 표시 생성 - 팩토리 메서드")
    fun `create read receipt using factory method`() {
        // given
        val messageId = MessageId.from("msg-1")
        val roomId = ChatRoomId.from(1L)
        val userId = UserId.from(100L)

        // when
        val readReceipt = MessageReadReceipt.create(messageId, roomId, userId)

        // then
        assertThat(readReceipt.messageId).isEqualTo(messageId)
        assertThat(readReceipt.roomId).isEqualTo(roomId)
        assertThat(readReceipt.userId).isEqualTo(userId)
        assertThat(readReceipt.id).isNull() // 새 엔티티는 ID가 null
        assertThat(readReceipt.readAt).isNotNull()
        assertThat(readReceipt.createdAt).isNotNull()
    }

    @Test
    @DisplayName("읽음 표시 생성 - 생성자 직접 호출")
    fun `create read receipt using constructor`() {
        // given
        val messageId = MessageId.from("msg-2")
        val roomId = ChatRoomId.from(2L)
        val userId = UserId.from(200L)
        val now = Instant.now()

        // when
        val readReceipt = MessageReadReceipt(
            id = MessageReadReceiptId.from(1L),
            messageId = messageId,
            roomId = roomId,
            userId = userId,
            readAt = now,
            createdAt = now
        )

        // then
        assertThat(readReceipt.id?.value).isEqualTo(1L)
        assertThat(readReceipt.messageId).isEqualTo(messageId)
        assertThat(readReceipt.roomId).isEqualTo(roomId)
        assertThat(readReceipt.userId).isEqualTo(userId)
        assertThat(readReceipt.readAt).isEqualTo(now)
        assertThat(readReceipt.createdAt).isEqualTo(now)
    }

    @Test
    @DisplayName("읽음 표시 - ID 참조 패턴 확인")
    fun `read receipt uses id references not object references`() {
        // given
        val messageId = MessageId.from("msg-3")
        val roomId = ChatRoomId.from(3L)
        val userId = UserId.from(300L)

        // when
        val readReceipt = MessageReadReceipt.create(messageId, roomId, userId)

        // then
        // MessageReadReceipt은 다른 Aggregate를 ID로만 참조해야 함
        assertThat(readReceipt.messageId).isInstanceOf(MessageId::class.java)
        assertThat(readReceipt.roomId).isInstanceOf(ChatRoomId::class.java)
        assertThat(readReceipt.userId).isInstanceOf(UserId::class.java)
    }

    @Test
    @DisplayName("읽음 표시 - 불변성 확인")
    fun `read receipt is immutable`() {
        // given
        val messageId = MessageId.from("msg-4")
        val roomId = ChatRoomId.from(4L)
        val userId = UserId.from(400L)
        val readReceipt = MessageReadReceipt.create(messageId, roomId, userId)

        // when
        val copy = readReceipt.copy(userId = UserId.from(500L))

        // then
        // 원본은 변경되지 않음
        assertThat(readReceipt.userId).isEqualTo(userId)
        assertThat(copy.userId).isEqualTo(UserId.from(500L))
    }

    @Test
    @DisplayName("읽음 표시 ID - Value Object 동등성")
    fun `read receipt id value object equality`() {
        // given
        val id1 = MessageReadReceiptId.from(1L)
        val id2 = MessageReadReceiptId.from(1L)
        val id3 = MessageReadReceiptId.from(2L)

        // then
        assertThat(id1).isEqualTo(id2)
        assertThat(id1).isNotEqualTo(id3)
        assertThat(id1.value).isEqualTo(1L)
    }

    @Test
    @DisplayName("읽음 표시 - 시간 정보 자동 설정")
    fun `read receipt timestamps are set automatically`() {
        // given
        val before = Instant.now().minusSeconds(1)

        // when
        val readReceipt = MessageReadReceipt.create(
            MessageId.from("msg-5"),
            ChatRoomId.from(5L),
            UserId.from(500L)
        )

        // then
        val after = Instant.now().plusSeconds(1)
        assertThat(readReceipt.readAt).isBetween(before, after)
        assertThat(readReceipt.createdAt).isBetween(before, after)
    }
}
