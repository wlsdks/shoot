package com.stark.shoot.application.port.out.user

import com.stark.shoot.domain.chat.user.User

interface FindUserPort {
    fun findByUsername(username: String): User?
    fun findUserById(userId: Long): User?
    fun findAll(): List<User>
    fun findByUserCode(userCode: String): User?
    fun findRandomUsers(excludeUserId: Long, limit: Int): List<User>
    fun existsById(userId: Long): Boolean

    // 친구 관계 정보를 함께 조회하는 메서드들
    fun findUserWithFriendshipsById(userId: Long): User?
    fun findUserWithFriendRequestsById(userId: Long): User?
    fun findUserWithAllRelationshipsById(userId: Long): User?

    // 친구 관계 확인을 위한 단순 조회 메서드들
    fun checkFriendship(userId: Long, friendId: Long): Boolean
    fun checkOutgoingFriendRequest(userId: Long, targetId: Long): Boolean
    fun checkIncomingFriendRequest(userId: Long, requesterId: Long): Boolean
}