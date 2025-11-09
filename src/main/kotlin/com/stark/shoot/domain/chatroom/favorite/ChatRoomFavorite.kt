package com.stark.shoot.domain.chatroom.favorite

import com.stark.shoot.domain.chatroom.favorite.vo.ChatRoomFavoriteId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.AggregateRoot
import java.time.Instant

/**
 * 채팅방 즐겨찾기 Aggregate Root
 *
 * DDD 개선: ChatRoom에서 분리
 * - 사용자의 개인 설정으로 ChatRoom의 상태가 아님
 * - 각 사용자의 즐겨찾기 설정이 독립적으로 관리됨
 * - 동시성 충돌 제거 (여러 사용자가 동시에 즐겨찾기 가능)
 *
 * @property id 즐겨찾기 ID
 * @property userId 사용자 ID
 * @property chatRoomId 채팅방 ID
 * @property isPinned 고정 여부
 * @property pinnedAt 고정 시간
 * @property displayOrder 표시 순서 (null = 자동 정렬)
 * @property createdAt 생성 시간
 */
@AggregateRoot
data class ChatRoomFavorite(
    val id: ChatRoomFavoriteId? = null,
    val userId: UserId,
    val chatRoomId: ChatRoomId,
    var isPinned: Boolean = true,
    var pinnedAt: Instant = Instant.now(),
    var displayOrder: Int? = null,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        /**
         * 채팅방 즐겨찾기 생성
         *
         * @param userId 사용자 ID
         * @param chatRoomId 채팅방 ID
         * @param displayOrder 표시 순서 (선택)
         * @return 생성된 ChatRoomFavorite
         */
        fun create(
            userId: UserId,
            chatRoomId: ChatRoomId,
            displayOrder: Int? = null
        ): ChatRoomFavorite {
            return ChatRoomFavorite(
                userId = userId,
                chatRoomId = chatRoomId,
                isPinned = true,
                pinnedAt = Instant.now(),
                displayOrder = displayOrder
            )
        }
    }

    /**
     * 즐겨찾기 핀 해제
     */
    fun unpin() {
        this.isPinned = false
    }

    /**
     * 즐겨찾기 다시 핀
     */
    fun repin() {
        if (!this.isPinned) {
            this.isPinned = true
            this.pinnedAt = Instant.now()
        }
    }

    /**
     * 표시 순서 변경
     *
     * @param newOrder 새 순서
     */
    fun updateDisplayOrder(newOrder: Int) {
        this.displayOrder = newOrder
    }
}
