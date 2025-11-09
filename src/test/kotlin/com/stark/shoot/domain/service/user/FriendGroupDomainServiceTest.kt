package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.social.service.group.FriendGroupDomainService
import com.stark.shoot.domain.shared.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("친구 그룹 도메인 서비스 테스트")
class FriendGroupDomainServiceTest {
    private val service = FriendGroupDomainService()

    @Test
    @DisplayName("[bad] 빈 이름으로 그룹을 생성하면 예외가 발생한다")
    fun `빈 이름으로 그룹을 생성하면 예외`() {
        assertThrows<IllegalArgumentException> { service.create(UserId.from(1L), "", null) }
    }

    @Test
    @DisplayName("[happy] 그룹 이름을 변경할 수 있다")
    fun `그룹 이름을 변경할 수 있다`() {
        val group = service.create(UserId.from(1L), "g1", null)
        val updated = service.rename(group, "new")
        assertThat(updated.name.value).isEqualTo("new")
    }
}
