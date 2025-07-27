package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.rest.dto.social.friend.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.command.SearchFriendsCommand

interface FriendSearchUseCase {
    /**
     * 잠재적 친구 검색
     *
     * @param command 친구 검색 커맨드
     * @return 친구 목록
     */
    fun searchPotentialFriends(command: SearchFriendsCommand): List<FriendResponse>
}
