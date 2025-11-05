package com.stark.shoot.application.service.util

/**
 * ChatRoomId 컨버터
 *
 * DDD 개선: Context 간 경계에서 ChatRoomId 변환
 * - Chat Context와 ChatRoom Context는 각각 독립적인 ChatRoomId VO를 가짐
 * - Application Layer에서 Context 간 이동 시 변환 필요
 * - Phase 2-3에서 Anti-Corruption Layer로 발전 예정
 *
 * 향후 개선:
 * - MSA 환경에서는 ACL(Anti-Corruption Layer)로 확장
 * - 현재는 단순 타입 변환이지만, 향후 검증/변환 로직 추가 가능
 */
object ChatRoomIdConverter {

    /**
     * ChatRoom Context의 ChatRoomId를 Chat Context의 ChatRoomId로 변환
     */
    fun toChat(chatRoomId: com.stark.shoot.domain.chatroom.vo.ChatRoomId): com.stark.shoot.domain.chat.vo.ChatRoomId {
        return com.stark.shoot.domain.chat.vo.ChatRoomId.from(chatRoomId.value)
    }

    /**
     * Chat Context의 ChatRoomId를 ChatRoom Context의 ChatRoomId로 변환
     */
    fun toChatRoom(chatRoomId: com.stark.shoot.domain.chat.vo.ChatRoomId): com.stark.shoot.domain.chatroom.vo.ChatRoomId {
        return com.stark.shoot.domain.chatroom.vo.ChatRoomId.from(chatRoomId.value)
    }
}

/**
 * Extension function: ChatRoom Context → Chat Context
 */
fun com.stark.shoot.domain.chatroom.vo.ChatRoomId.toChat(): com.stark.shoot.domain.chat.vo.ChatRoomId =
    ChatRoomIdConverter.toChat(this)

/**
 * Extension function: Chat Context → ChatRoom Context
 */
fun com.stark.shoot.domain.chat.vo.ChatRoomId.toChatRoom(): com.stark.shoot.domain.chatroom.vo.ChatRoomId =
    ChatRoomIdConverter.toChatRoom(this)
