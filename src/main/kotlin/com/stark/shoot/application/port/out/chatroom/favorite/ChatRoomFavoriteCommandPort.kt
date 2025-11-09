package com.stark.shoot.application.port.out.chatroom.favorite

import com.stark.shoot.domain.chatroom.favorite.ChatRoomFavorite
import com.stark.shoot.domain.chatroom.favorite.vo.ChatRoomFavoriteId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId

/**
 * 채팅방 즐겨찾기 명령 포트
 *
 * ChatRoomFavorite Aggregate의 저장/삭제 작업을 담당하는 outbound port입니다.
 */
interface ChatRoomFavoriteCommandPort {
    /**
     * 채팅방 즐겨찾기 정보를 저장합니다.
     *
     * @param chatRoomFavorite 저장할 즐겨찾기 정보
     * @return 저장된 즐겨찾기 정보
     */
    fun save(chatRoomFavorite: ChatRoomFavorite): ChatRoomFavorite

    /**
     * 채팅방 즐겨찾기 정보를 삭제합니다.
     *
     * @param id 삭제할 즐겨찾기 ID
     */
    fun delete(id: ChatRoomFavoriteId)

    /**
     * 사용자의 특정 채팅방 즐겨찾기를 삭제합니다.
     *
     * @param userId 사용자 ID
     * @param chatRoomId 채팅방 ID
     */
    fun deleteByUserIdAndChatRoomId(userId: UserId, chatRoomId: ChatRoomId)

    /**
     * 사용자의 모든 즐겨찾기를 삭제합니다.
     *
     * @param userId 사용자 ID
     */
    fun deleteAllByUserId(userId: UserId)

    /**
     * 특정 채팅방의 모든 즐겨찾기를 삭제합니다.
     *
     * @param chatRoomId 채팅방 ID
     */
    fun deleteAllByChatRoomId(chatRoomId: ChatRoomId)
}
