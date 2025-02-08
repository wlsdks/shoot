package com.stark.shoot.application.port.`in`.user

import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId

interface RetrieveUserUseCase {
    fun findById(id: ObjectId): User?
    fun findByUsername(username: String): User?
    fun findByUserCode(userCode: String): User?

    // 자기 자신을 제외한 임의의 유저들 N명 조회
    fun findRandomUsers(excludeId: ObjectId, limit: Int): List<User>
}