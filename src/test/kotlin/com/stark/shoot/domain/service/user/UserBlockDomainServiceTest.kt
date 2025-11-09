package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.social.service.block.UserBlockDomainService
import com.stark.shoot.domain.user.vo.Nickname
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.user.vo.Username
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("사용자 차단 도메인 서비스 테스트")
class UserBlockDomainServiceTest {
    private val service = UserBlockDomainService()

    @Test
    @DisplayName("[bad] 자신을 차단하면 예외가 발생한다")
    fun `자신을 차단하면 예외`() {
        val user = User(
            id = UserId.from(1L),
            username = Username.from("user123"),
            nickname = Nickname.from("User"),
            userCode = UserCode.from("USER1234")
        )
        assertThrows<IllegalArgumentException> { service.block(user.id!!, UserId.from(1L)) }
    }
}
