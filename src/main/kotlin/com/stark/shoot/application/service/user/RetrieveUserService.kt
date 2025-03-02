package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.RetrieveUserUseCase
import com.stark.shoot.application.port.out.user.RetrieveUserPort
import com.stark.shoot.domain.chat.user.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class RetrieveUserService(
    private val retrieveUserPort: RetrieveUserPort
) : RetrieveUserUseCase {

    /**
     * 유저 조회
     */
    override fun findById(
        id: ObjectId
    ): User? {
        return retrieveUserPort.findById(id)
    }

    /**
     * 사용자명으로 사용자 조회
     */
    override fun findByUsername(
        username: String
    ): User? {
        return retrieveUserPort.findByUsername(username)
    }

    /**
     * 사용자 코드로 사용자 조회
     */
    override fun findByUserCode(
        userCode: String
    ): User? {
        return retrieveUserPort.findByUserCode(userCode)
    }

    /**
     * 자기 자신을 제외한 임의의 유저들 N명 조회
     */
    override fun findRandomUsers(
        excludeId: ObjectId,
        limit: Int
    ): List<User> {
        return retrieveUserPort.findRandomUsers(excludeId, limit)
    }

    /**
     * 사용자명 또는 사용자 코드로 사용자 조회
     */
    override fun findUserByUsernameOrUserCode(
        query: String
    ): List<User> {
        return retrieveUserPort.findByUsernameOrUserCode(query)
    }

}