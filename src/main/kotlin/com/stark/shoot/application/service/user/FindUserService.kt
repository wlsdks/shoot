package com.stark.shoot.application.service.user

import com.stark.shoot.application.port.`in`.user.FindUserUseCase
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.user.vo.Username
import com.stark.shoot.infrastructure.annotation.UseCase

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
    override fun findById(userId: UserId): User? {
        return findUserPort.findUserById(userId)
    }

    /**
     * 사용자명으로 사용자 조회
     *
     * @param username 사용자명
     * @return 사용자 정보
     */
    override fun findByUsername(username: Username): User? {
        return findUserPort.findByUsername(username)
    }

    /**
     * 사용자 코드로 사용자 조회
     *
     * @param userCode 사용자 코드
     * @return 사용자 정보
     */
    override fun findByUserCode(userCode: UserCode): User? {
        return findUserPort.findByUserCode(userCode)
    }

}