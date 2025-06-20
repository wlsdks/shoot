package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.domain.chat.user.UserCode
import com.stark.shoot.domain.service.user.block.UserBlockDomainService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("사용자 차단 도메인 서비스 테스트")
class UserBlockDomainServiceTest {
    private val service = UserBlockDomainService()

    @Test
    fun `자신을 차단하면 예외`() {
        val user = User(id=1L, username="a", nickname="A", userCode=UserCode.from("A1"))
        assertThrows<IllegalArgumentException> { service.block(user,1L) }
    }

    @Test
    fun `차단과 해제가 가능하다`() {
        val user = User(id=1L, username="a", nickname="A", userCode=UserCode.from("A1"))
        val blocked = service.block(user,2L)
        assertThat(blocked.blockedUserIds).contains(2L)
        val unblocked = service.unblock(blocked,2L)
        assertThat(unblocked.blockedUserIds).doesNotContain(2L)
    }
}
