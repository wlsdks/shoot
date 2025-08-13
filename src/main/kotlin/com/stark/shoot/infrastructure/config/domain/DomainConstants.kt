package com.stark.shoot.infrastructure.config.domain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

/**
 * 도메인 전반에서 사용되는 상수값들을 설정으로 관리
 */
@ConfigurationProperties(prefix = "app.domain")
data class DomainConstants @ConstructorBinding constructor(
    val chatRoom: ChatRoomConstants = ChatRoomConstants(),
    val message: MessageConstants = MessageConstants(),
    val friend: FriendConstants = FriendConstants()
) {
    data class ChatRoomConstants(
        val maxParticipants: Int = 100,
        val minGroupParticipants: Int = 2,
        val maxPinnedMessages: Int = 5
    )
    
    data class MessageConstants(
        val maxContentLength: Int = 4000,
        val maxAttachmentSize: Long = 50 * 1024 * 1024, // 50MB
        val batchSize: Int = 100
    )
    
    data class FriendConstants(
        val maxFriendCount: Int = 1000,
        val recommendationLimit: Int = 20
    )
}