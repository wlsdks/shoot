package com.stark.shoot.domain.social.constants

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Social Context 전용 상수
 */
@ConfigurationProperties(prefix = "app.domain.friend")
data class FriendConstants(
    val maxFriendCount: Int = 1000,
    val recommendationLimit: Int = 20
)
