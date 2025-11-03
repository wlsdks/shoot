package com.stark.shoot.infrastructure.config.domain

import com.stark.shoot.domain.chat.message.service.*
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.domain.chatroom.service.ChatRoomEventService
import com.stark.shoot.domain.chatroom.service.ChatRoomMetadataDomainService
import com.stark.shoot.domain.chatroom.service.ChatRoomValidationDomainService
import com.stark.shoot.domain.notification.service.NotificationDomainService
import com.stark.shoot.domain.social.service.FriendDomainService
import com.stark.shoot.domain.social.service.block.UserBlockDomainService
import com.stark.shoot.domain.social.service.group.FriendGroupDomainService
import com.stark.shoot.domain.constants.DomainConstants
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 도메인 서비스 빈 등록을 위한 설정 클래스.
 * Domain 계층을 프레임워크와 분리하기 위해 @Service 어노테이션을 제거하고
 * 이곳에서 명시적으로 빈을 정의한다.
 */
@Configuration
class DomainServiceConfig(
    private val domainConstants: DomainConstants
) {

    @Bean fun friendDomainService() = FriendDomainService()

    @Bean fun friendGroupDomainService() = FriendGroupDomainService()

    @Bean fun userBlockDomainService() = UserBlockDomainService()

    @Bean fun chatRoomDomainService() = ChatRoomDomainService()

    @Bean fun chatRoomEventService() = ChatRoomEventService()

    @Bean fun chatRoomMetadataDomainService() = ChatRoomMetadataDomainService()

    @Bean fun chatRoomValidationDomainService() = ChatRoomValidationDomainService(domainConstants)

    @Bean fun messageDomainService() = MessageDomainService()

    @Bean fun messageEditDomainService() = MessageEditDomainService()

    @Bean fun messageForwardDomainService() = MessageForwardDomainService()

    @Bean fun messagePinDomainService() = MessagePinDomainService()

    @Bean fun messageReactionService() = MessageReactionService()

    @Bean fun notificationDomainService() = NotificationDomainService()
}
