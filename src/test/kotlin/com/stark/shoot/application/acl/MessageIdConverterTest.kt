package com.stark.shoot.application.acl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * MessageIdConverter 테스트
 *
 * TDD Red-Green-Refactor 사이클:
 * 1. RED: 테스트 작성 (실패)
 * 2. GREEN: 최소 구현
 * 3. REFACTOR: 코드 개선
 */
@DisplayName("MessageIdConverter 테스트")
class MessageIdConverterTest {

    @Test
    @DisplayName("Chat Context MessageId를 ChatRoom Context MessageId로 변환")
    fun `convert chat message id to chatroom message id`() {
        // Given
        val chatMessageId = com.stark.shoot.domain.chat.message.vo.MessageId.from("msg_12345")

        // When
        val chatRoomMessageId = MessageIdConverter.toMessageId(chatMessageId)

        // Then
        assertThat(chatRoomMessageId.value).isEqualTo("msg_12345")
        assertThat(chatRoomMessageId).isInstanceOf(com.stark.shoot.domain.chatroom.vo.MessageId::class.java)
    }

    @Test
    @DisplayName("ChatRoom Context MessageId를 Chat Context MessageId로 변환")
    fun `convert chatroom message id to chat message id`() {
        // Given
        val chatRoomMessageId = com.stark.shoot.domain.chatroom.vo.MessageId.from("msg_67890")

        // When
        val chatMessageId = MessageIdConverter.toChatMessageId(chatRoomMessageId)

        // Then
        assertThat(chatMessageId.value).isEqualTo("msg_67890")
        assertThat(chatMessageId).isInstanceOf(com.stark.shoot.domain.chat.message.vo.MessageId::class.java)
    }

    @Test
    @DisplayName("Extension function: Chat MessageId -> ChatRoom MessageId")
    fun `extension function toMessageId`() {
        // Given
        val chatMessageId = com.stark.shoot.domain.chat.message.vo.MessageId.from("msg_ext_123")

        // When
        val chatRoomMessageId = chatMessageId.toMessageId()

        // Then
        assertThat(chatRoomMessageId.value).isEqualTo("msg_ext_123")
    }

    @Test
    @DisplayName("Extension function: ChatRoom MessageId -> Chat MessageId")
    fun `extension function toChatMessageId`() {
        // Given
        val chatRoomMessageId = com.stark.shoot.domain.chatroom.vo.MessageId.from("msg_ext_456")

        // When
        val chatMessageId = chatRoomMessageId.toChatMessageId()

        // Then
        assertThat(chatMessageId.value).isEqualTo("msg_ext_456")
    }

    @Test
    @DisplayName("양방향 변환 후 원래 값과 동일")
    fun `bidirectional conversion preserves value`() {
        // Given
        val originalId = "msg_bidirectional_789"
        val chatMessageId = com.stark.shoot.domain.chat.message.vo.MessageId.from(originalId)

        // When: Chat -> ChatRoom -> Chat
        val converted = chatMessageId.toMessageId().toChatMessageId()

        // Then
        assertThat(converted.value).isEqualTo(originalId)
        assertThat(converted).isEqualTo(chatMessageId)
    }
}
