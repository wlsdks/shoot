package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.web.dto.user.FriendResponse

interface FindFriendUseCase {
    fun getFriends(currentUserId: Long): List<FriendResponse> // 친구 목록 조회
    fun getIncomingFriendRequests(currentUserId: Long): List<FriendResponse>
    fun getOutgoingFriendRequests(currentUserId: Long): List<FriendResponse>
}