package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.FindUserUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class FindUserService(
    private val findUserPort: FindUserPort
) : FindUserUseCase {

    /**
     * 유저 조회
     */
    override fun findById(
        id: ObjectId
    ): User? {
        return findUserPort.findUserById(id)
    }

    /**
     * 사용자명으로 사용자 조회
     */
    override fun findByUsername(
        username: String
    ): User? {
        return findUserPort.findByUsername(username)
    }

    /**
     * 사용자 코드로 사용자 조회
     */
    override fun findByUserCode(
        userCode: String
    ): User? {
        return findUserPort.findByUserCode(userCode)
    }

    /**
     * 자기 자신을 제외한 임의의 유저들 N명 조회
     */
    override fun findRandomUsers(
        excludeId: ObjectId,
        limit: Int
    ): List<User> {
        return findUserPort.findRandomUsers(excludeId, limit)
    }

    /**
     * 사용자명 또는 사용자 코드로 사용자 조회
     */
    override fun findUserByUsernameOrUserCode(
        query: String
    ): List<User> {
        return findUserPort.findByUsernameOrUserCode(query)
    }

}