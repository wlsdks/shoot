package com.stark.shoot.application.port.out.user.token

import com.stark.shoot.domain.user.RefreshToken
import com.stark.shoot.domain.user.vo.RefreshTokenValue
import com.stark.shoot.domain.exception.web.MongoOperationException

interface RefreshTokenQueryPort {
    @Throws(MongoOperationException::class)
    fun findByToken(token: RefreshTokenValue): RefreshToken?
}
