package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse
import com.stark.shoot.domain.common.vo.UserId

interface FindFriendUseCase {
    fun getFriends(currentUserId: UserId): List<FriendResponse> // 친구 목록 조회
    fun getIncomingFriendRequests(currentUserId: UserId): List<FriendResponse>
    fun getOutgoingFriendRequests(currentUserId: UserId): List<FriendResponse>
}