package com.stark.shoot.application.port.out.user.friend

import com.stark.shoot.domain.user.vo.UserId

interface FriendCommandPort {
    fun removeOutgoingFriendRequest(userId: UserId, targetUserId: UserId)
    fun removeIncomingFriendRequest(userId: UserId, fromUserId: UserId)
    fun addFriendRelation(userId: UserId, friendId: UserId)
    fun removeFriendRelation(userId: UserId, friendId: UserId)

    /**
     * 사용자의 모든 친구 관계를 삭제합니다.
     * 양방향 친구 관계 모두 제거됩니다.
     *
     * @param userId 삭제할 사용자 ID
     */
    fun deleteAllFriendships(userId: UserId)
}
