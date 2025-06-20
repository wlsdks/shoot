package com.stark.shoot.domain.service.message

import com.stark.shoot.domain.chat.event.ChatEvent
import com.stark.shoot.domain.chat.event.type.EventType
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.UrlPreview
import com.stark.shoot.domain.common.vo.MessageId
import com.stark.shoot.domain.chat.room.vo.ChatRoomId
import com.stark.shoot.domain.common.vo.UserId
import java.util.*

/**
 * 메시지 관련 도메인 서비스
 * 메시지 생성, 처리, 이벤트 생성 등의 도메인 로직을 담당합니다.
 */
class MessageDomainService {

    /**
     * 메시지 요청으로부터 도메인 메시지 객체를 생성하고 처리합니다.
     *
     * @param messageRequest 메시지 요청 DTO
     * @param extractUrls URL 추출 함수
     * @param getCachedPreview 캐시된 미리보기 조회 함수
     * @return 처리된 도메인 메시지 객체
     */
    fun createAndProcessMessage(
        roomId: ChatRoomId,
        senderId: UserId,
        contentText: String,
        contentType: com.stark.shoot.domain.chat.message.type.MessageType,
        threadId: MessageId? = null,
        extractUrls: (String) -> List<String>,
        getCachedPreview: (String) -> UrlPreview?
    ): ChatMessage {
        // 1. 도메인 객체 생성
        val tempId = UUID.randomUUID().toString()
        val chatMessage = ChatMessage.create(
            roomId = roomId,
            senderId = senderId,
            text = contentText,
            type = contentType,
            tempId = tempId,
            threadId = threadId
        )

        // 2. URL 미리보기 처리 (도메인 로직 활용)
        return ChatMessage.processUrlPreview(
            message = chatMessage,
            extractUrls = extractUrls,
            getCachedPreview = getCachedPreview
        )
    }

    /**
     * 도메인 메시지 객체로부터 도메인 이벤트를 생성합니다.
     *
     * @param chatMessage 도메인 메시지 객체
     * @return 생성된 도메인 이벤트
     */
    fun createMessageEvent(chatMessage: ChatMessage): ChatEvent {
        return ChatEvent.fromMessage(chatMessage, EventType.MESSAGE_CREATED)
    }

}
