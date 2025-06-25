package com.stark.shoot.domain.user

import com.stark.shoot.domain.user.type.FriendRequestStatus
import com.stark.shoot.domain.user.type.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("유저 관련 enum 테스트")
class UserEnumsTest {

    @Test
    @DisplayName("[happy] FriendRequestStatus 값 확인")
    fun `FriendRequestStatus 값 확인`() {
        val names = FriendRequestStatus.values().map { it.name }
        assertThat(names).containsExactly("PENDING", "ACCEPTED", "REJECTED", "CANCELLED")
    }

    @Test
    @DisplayName("[happy] UserStatus 값 확인")
    fun `UserStatus 값 확인`() {
        val names = UserStatus.values().map { it.name }
        assertThat(names).containsExactly("OFFLINE", "ONLINE", "BUSY", "AWAY", "INVISIBLE", "DO_NOT_DISTURB", "IDLE")
    }
}
