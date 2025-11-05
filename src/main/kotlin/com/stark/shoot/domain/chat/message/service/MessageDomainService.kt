package com.stark.shoot.domain.chat.message.service

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chat.message.vo.ChatMessageMetadata
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.event.MessageEvent
import com.stark.shoot.domain.shared.event.type.EventType
import com.stark.shoot.domain.shared.UserId
import java.util.*

/**
 * 메시지 관련 도메인 서비스
 * 메시지 생성, 처리, 이벤트 생성 등의 도메인 로직을 담당합니다.
 */
class MessageDomainService {

    /**
     * 메시지 요청으로부터 도메인 메시지 객체를 생성하고 처리합니다.
     *
     * @param roomId 채팅방 ID
     * @param senderId 발신자 ID
     * @param contentText 메시지 내용
     * @param contentType 메시지 타입
     * @param tempId 임시 ID (프론트엔드에서 제공, null이면 새로 생성)
     * @param threadId 스레드 ID (선택)
     * @param extractUrls URL 추출 함수
     * @param getCachedPreview 캐시된 미리보기 조회 함수
     * @return 처리된 도메인 메시지 객체
     */
    fun createAndProcessMessage(
        roomId: ChatRoomId,
        senderId: UserId,
        contentText: String,
        contentType: MessageType,
        tempId: String? = null,
        threadId: MessageId? = null,
        extractUrls: (String) -> List<String>,
        getCachedPreview: (String) -> ChatMessageMetadata.UrlPreview?
    ): ChatMessage {
        // 1. 도메인 객체 생성 (기존 tempId 사용 또는 새로 생성)
        val finalTempId = tempId ?: UUID.randomUUID().toString() // 기존 tempId 우선 사용
        val chatMessage = ChatMessage.create(
            roomId = roomId,
            senderId = senderId,
            text = contentText,
            type = contentType,
            tempId = finalTempId, // 프론트엔드 tempId 유지
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
    fun createMessageEvent(chatMessage: ChatMessage): MessageEvent {
        return MessageEvent.fromMessage(chatMessage, EventType.MESSAGE_CREATED)
    }

}
