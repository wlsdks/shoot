package com.stark.shoot.infrastructure.config.domain

import com.stark.shoot.domain.chatroom.constants.ChatRoomConstants
import com.stark.shoot.domain.chat.constants.MessageConstants
import com.stark.shoot.domain.social.constants.FriendConstants
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 도메인 설정 프로퍼티를 활성화하는 설정 클래스
 */
@Configuration
@EnableConfigurationProperties(
    ChatRoomConstants::class,
    MessageConstants::class,
    FriendConstants::class
)
class DomainPropertiesConfig