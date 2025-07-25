package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.user.vo.Username

interface FindUserPort {
    fun findByUsername(username: Username): User?
    fun findUserById(userId: UserId): User?
    fun findAll(): List<User>
    fun findByUserCode(userCode: UserCode): User?
    fun existsById(userId: UserId): Boolean

    // 친구 관계 확인을 위한 단순 조회 메서드들
    fun checkFriendship(userId: UserId, friendId: UserId): Boolean
    fun checkOutgoingFriendRequest(userId: UserId, targetId: UserId): Boolean
    fun checkIncomingFriendRequest(userId: UserId, requesterId: UserId): Boolean
}