package com.stark.shoot.application.port.out.chatroom.favorite

import com.stark.shoot.domain.chatroom.favorite.ChatRoomFavorite
import com.stark.shoot.domain.chatroom.favorite.vo.ChatRoomFavoriteId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * 채팅방 즐겨찾기 조회 포트
 *
 * ChatRoomFavorite Aggregate의 조회 작업을 담당하는 outbound port입니다.
 */
interface ChatRoomFavoriteQueryPort {
    /**
     * ID로 즐겨찾기 정보를 조회합니다.
     *
     * @param id 즐겨찾기 ID
     * @return 즐겨찾기 정보 (없으면 null)
     */
    fun findById(id: ChatRoomFavoriteId): ChatRoomFavorite?

    /**
     * 사용자의 특정 채팅방 즐겨찾기를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param chatRoomId 채팅방 ID
     * @return 즐겨찾기 정보 (없으면 null)
     */
    fun findByUserIdAndChatRoomId(userId: UserId, chatRoomId: ChatRoomId): ChatRoomFavorite?

    /**
     * 사용자의 모든 즐겨찾기를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pinnedOnly true인 경우 고정된 것만 조회
     * @return 즐겨찾기 목록 (displayOrder, pinnedAt 기준 정렬)
     */
    fun findAllByUserId(userId: UserId, pinnedOnly: Boolean = false): List<ChatRoomFavorite>

    /**
     * 사용자의 고정된 즐겨찾기 개수를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 고정된 즐겨찾기 개수
     */
    fun countPinnedByUserId(userId: UserId): Long

    /**
     * 사용자가 특정 채팅방을 즐겨찾기 했는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @param chatRoomId 채팅방 ID
     * @return 즐겨찾기 여부
     */
    fun existsByUserIdAndChatRoomId(userId: UserId, chatRoomId: ChatRoomId): Boolean

    /**
     * 채팅방이 즐겨찾기 되어있는지 확인합니다 (누구라도).
     *
     * @param chatRoomId 채팅방 ID
     * @return 즐겨찾기 여부
     */
    fun existsByChatRoomId(chatRoomId: ChatRoomId): Boolean
}
