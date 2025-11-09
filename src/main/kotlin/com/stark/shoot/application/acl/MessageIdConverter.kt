package com.stark.shoot.application.acl

/**
 * MessageId Anti-Corruption Layer (ACL)
 *
 * DDD 패턴: Context 간 경계에서 MessageId 변환
 * - Chat Context와 ChatRoom Context는 각각 독립적인 MessageId VO를 가짐
 * - Application Layer ACL에서 Context 간 이동 시 변환 수행
 * - 각 Context의 도메인 모델이 다른 Context의 영향을 받지 않도록 보호
 *
 * ACL 역할:
 * 1. 타입 변환: 구조적으로 동일하지만 타입이 다른 VO 간 변환
 * 2. 도메인 보호: 외부 Context의 변경이 내부 도메인에 영향을 주지 않도록 방어
 * 3. MSA 준비: 향후 서비스 분리 시 API 경계에서 DTO 변환 역할
 */
object MessageIdConverter {

    /**
     * Chat Context의 MessageId를 ChatRoom Context의 MessageId로 변환
     *
     * @param chatMessageId Chat Context의 MessageId
     * @return ChatRoom Context의 MessageId
     */
    fun toMessageId(
        chatMessageId: com.stark.shoot.domain.chat.message.vo.MessageId
    ): com.stark.shoot.domain.chatroom.vo.MessageId {
        return com.stark.shoot.domain.chatroom.vo.MessageId.from(chatMessageId.value)
    }

    /**
     * ChatRoom Context의 MessageId를 Chat Context의 MessageId로 변환
     *
     * @param messageId ChatRoom Context의 MessageId
     * @return Chat Context의 MessageId
     */
    fun toChatMessageId(
        messageId: com.stark.shoot.domain.chatroom.vo.MessageId
    ): com.stark.shoot.domain.chat.message.vo.MessageId {
        return com.stark.shoot.domain.chat.message.vo.MessageId.from(messageId.value)
    }
}

/**
 * Extension function: Chat Context → ChatRoom Context
 */
fun com.stark.shoot.domain.chat.message.vo.MessageId.toMessageId(): com.stark.shoot.domain.chatroom.vo.MessageId =
    MessageIdConverter.toMessageId(this)

/**
 * Extension function: ChatRoom Context → Chat Context
 */
fun com.stark.shoot.domain.chatroom.vo.MessageId.toChatMessageId(): com.stark.shoot.domain.chat.message.vo.MessageId =
    MessageIdConverter.toChatMessageId(this)
