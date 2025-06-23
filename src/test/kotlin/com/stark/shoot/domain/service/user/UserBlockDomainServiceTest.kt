package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.user.User
import com.stark.shoot.domain.user.service.block.UserBlockDomainService
import com.stark.shoot.domain.user.vo.Nickname
import com.stark.shoot.domain.user.vo.UserCode
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.domain.user.vo.Username
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("사용자 차단 도메인 서비스 테스트")
class UserBlockDomainServiceTest {
    private val service = UserBlockDomainService()

    @Test
    fun `자신을 차단하면 예외`() {
        val user = User(
            id = UserId.from(1L),
            username = Username.from("a"),
            nickname = Nickname.from("A"),
            userCode = UserCode.from("A1")
        )
        assertThrows<IllegalArgumentException> { service.block(user.id!!, UserId.from(1L)) }
    }
}
