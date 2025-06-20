package com.stark.shoot.domain.service.user

import com.stark.shoot.domain.user.service.group.FriendGroupDomainService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("친구 그룹 도메인 서비스 테스트")
class FriendGroupDomainServiceTest {
    private val service = FriendGroupDomainService()

    @Test
    fun `빈 이름으로 그룹을 생성하면 예외`() {
        assertThrows<IllegalArgumentException> { service.create(1L, "", null) }
    }

    @Test
    fun `그룹 이름을 변경할 수 있다`() {
        val group = service.create(1L, "g1", null)
        val updated = service.rename(group, "new")
        assertThat(updated.name.value).isEqualTo("new")
    }
}
