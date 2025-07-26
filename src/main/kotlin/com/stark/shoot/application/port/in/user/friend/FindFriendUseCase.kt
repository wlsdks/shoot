package com.stark.shoot.application.port.`in`.user.friend

import com.stark.shoot.adapter.`in`.rest.dto.user.FriendResponse
import com.stark.shoot.application.port.`in`.user.friend.command.GetFriendsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetIncomingFriendRequestsCommand
import com.stark.shoot.application.port.`in`.user.friend.command.GetOutgoingFriendRequestsCommand

interface FindFriendUseCase {
    fun getFriends(command: GetFriendsCommand): List<FriendResponse> // 친구 목록 조회
    fun getIncomingFriendRequests(command: GetIncomingFriendRequestsCommand): List<FriendResponse>
    fun getOutgoingFriendRequests(command: GetOutgoingFriendRequestsCommand): List<FriendResponse>
}
