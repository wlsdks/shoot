package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.FindUserUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.UseCase
import org.bson.types.ObjectId

@UseCase
class FindUserService(
    private val findUserPort: FindUserPort
) : FindUserUseCase {

    /**
     * 사용자 ID로 사용자 조회
     *
     * @param id 사용자 ID
     * @return 사용자 정보
     */
    override fun findById(
        id: ObjectId
    ): User? {
        return findUserPort.findUserById(id)
    }

    /**
     * 사용자명으로 사용자 조회
     *
     * @param username 사용자명
     * @return 사용자 정보
     */
    override fun findByUsername(
        username: String
    ): User? {
        return findUserPort.findByUsername(username)
    }

    /**
     * 사용자 코드로 사용자 조회
     *
     * @param userCode 사용자 코드
     * @return 사용자 정보
     */
    override fun findByUserCode(
        userCode: String
    ): User? {
        return findUserPort.findByUserCode(userCode)
    }


    /**
     * 사용자명 또는 사용자 코드로 사용자 조회
     *
     * @param excludeId 제외할 사용자 ID
     * @param limit 조회할 사용자 수
     * @return 사용자 목록
     */
    override fun findRandomUsers(
        excludeId: ObjectId,
        limit: Int
    ): List<User> {
        return findUserPort.findRandomUsers(excludeId, limit)
    }

    /**
     * 사용자명 또는 사용자 코드로 사용자 조회
     *
     * @param query 사용자명 또는 사용자 코드
     * @return 사용자 목록
     */
    override fun findUserByUsernameOrUserCode(
        query: String
    ): List<User> {
        return findUserPort.findByUsernameOrUserCode(query)
    }

}