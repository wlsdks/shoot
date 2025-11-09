package com.stark.shoot.domain.chatroom.favorite

import com.stark.shoot.domain.chatroom.favorite.vo.ChatRoomFavoriteId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("ChatRoomFavorite Aggregate 테스트")
class ChatRoomFavoriteTest {

    @Test
    @DisplayName("채팅방 즐겨찾기 생성 - 기본값")
    fun `create favorite with default values`() {
        // Given
        val userId = UserId.from(1L)
        val chatRoomId = ChatRoomId.from(100L)

        // When
        val favorite = ChatRoomFavorite.create(userId, chatRoomId)

        // Then
        assertThat(favorite.userId).isEqualTo(userId)
        assertThat(favorite.chatRoomId).isEqualTo(chatRoomId)
        assertThat(favorite.isPinned).isTrue()
        assertThat(favorite.displayOrder).isNull()
    }

    @Test
    @DisplayName("채팅방 즐겨찾기 생성 - 표시 순서 지정")
    fun `create favorite with display order`() {
        // Given
        val userId = UserId.from(1L)
        val chatRoomId = ChatRoomId.from(100L)
        val displayOrder = 5

        // When
        val favorite = ChatRoomFavorite.create(userId, chatRoomId, displayOrder)

        // Then
        assertThat(favorite.displayOrder).isEqualTo(5)
    }

    @Test
    @DisplayName("즐겨찾기 핀 해제")
    fun `unpin favorite`() {
        // Given
        val favorite = ChatRoomFavorite.create(
            userId = UserId.from(1L),
            chatRoomId = ChatRoomId.from(100L)
        )
        assertThat(favorite.isPinned).isTrue()

        // When
        favorite.unpin()

        // Then
        assertThat(favorite.isPinned).isFalse()
    }

    @Test
    @DisplayName("즐겨찾기 다시 핀")
    fun `repin favorite`() {
        // Given
        val favorite = ChatRoomFavorite.create(
            userId = UserId.from(1L),
            chatRoomId = ChatRoomId.from(100L)
        )
        favorite.unpin()
        assertThat(favorite.isPinned).isFalse()

        // When
        favorite.repin()

        // Then
        assertThat(favorite.isPinned).isTrue()
    }

    @Test
    @DisplayName("이미 핀된 상태에서 repin 호출 - 상태 유지")
    fun `repin already pinned favorite - no change`() {
        // Given
        val favorite = ChatRoomFavorite.create(
            userId = UserId.from(1L),
            chatRoomId = ChatRoomId.from(100L)
        )
        val originalPinnedAt = favorite.pinnedAt

        // When
        favorite.repin()

        // Then
        assertThat(favorite.isPinned).isTrue()
        assertThat(favorite.pinnedAt).isEqualTo(originalPinnedAt)
    }

    @Test
    @DisplayName("표시 순서 변경")
    fun `update display order`() {
        // Given
        val favorite = ChatRoomFavorite.create(
            userId = UserId.from(1L),
            chatRoomId = ChatRoomId.from(100L),
            displayOrder = 1
        )

        // When
        favorite.updateDisplayOrder(10)

        // Then
        assertThat(favorite.displayOrder).isEqualTo(10)
    }
}
