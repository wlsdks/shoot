package com.stark.shoot.infrastructure.config.filter

import com.stark.shoot.application.filter.message.chain.DefaultMessageProcessingChain
import com.stark.shoot.application.filter.message.impl.*
import com.stark.shoot.application.filter.common.MessageProcessingFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MessageProcessingConfig {

    @Bean
    fun messageFilters(
        chatRoomLoadFilter: ChatRoomLoadFilter,
        urlPreviewFilter: UrlPreviewFilter,
        readStatusInitFilter: ReadStatusInitFilter,
        unreadCountUpdateFilter: UnreadCountUpdateFilter,
        messageSaveFilter: MessageSaveFilter,
        chatRoomUpdateFilter: ChatRoomUpdateFilter,
        eventPublishFilter: EventPublishFilter
    ): List<MessageProcessingFilter> {
        // 필터 실행 순서 정의
        return listOf(
            chatRoomLoadFilter,        // 0. 채팅방 로딩 (다른 필터에서 사용)
            urlPreviewFilter,          // 1. URL 미리보기 처리
            unreadCountUpdateFilter,   // 2. 읽지 않은 메시지 수 업데이트
            messageSaveFilter,         // 3. 메시지 저장
            readStatusInitFilter,      // 4. 읽음 상태 초기화 (메시지 ID 필요)
            chatRoomUpdateFilter,      // 5. 채팅방 메타데이터 업데이트
            eventPublishFilter         // 6. 이벤트 발행
        )
    }

    @Bean
    fun messageProcessingChain(
        filters: List<MessageProcessingFilter>
    ): DefaultMessageProcessingChain {
        return DefaultMessageProcessingChain(filters)
    }

}
