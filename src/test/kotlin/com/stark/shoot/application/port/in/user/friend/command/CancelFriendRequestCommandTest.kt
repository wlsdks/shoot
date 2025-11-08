package com.stark.shoot.application.port.`in`.user.friend.command

import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.adapter.`in`.rest.dto.social.friend.CancelFriendRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CancelFriendRequestCommandTest {

    @Test
    fun `should create command with correct values`() {
        // Given
        val currentUserId = 1L
        val targetUserId = 2L

        // When
        val request = CancelFriendRequest(currentUserId, targetUserId)
        val command = CancelFriendRequestCommand.of(request)

        // Then
        assertEquals(UserId.from(currentUserId), command.currentUserId)
        assertEquals(UserId.from(targetUserId), command.targetUserId)
    }

    @Test
    fun `should create command directly with UserId objects`() {
        // Given
        val currentUserId = UserId.from(1L)
        val targetUserId = UserId.from(2L)

        // When
        val command = CancelFriendRequestCommand(currentUserId, targetUserId)

        // Then
        assertEquals(currentUserId, command.currentUserId)
        assertEquals(targetUserId, command.targetUserId)
    }
}