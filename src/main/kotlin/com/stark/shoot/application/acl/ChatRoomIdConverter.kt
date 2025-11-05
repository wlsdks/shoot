package com.stark.shoot.application.acl

/**
 * ChatRoomId Anti-Corruption Layer (ACL)
 *
 * DDD 패턴: Context 간 경계에서 ChatRoomId 변환
 * - Chat Context와 ChatRoom Context는 각각 독립적인 ChatRoomId VO를 가짐
 * - Application Layer ACL에서 Context 간 이동 시 변환 수행
 * - 각 Context의 도메인 모델이 다른 Context의 영향을 받지 않도록 보호
 *
 * ACL 역할:
 * 1. 타입 변환: 구조적으로 동일하지만 타입이 다른 VO 간 변환
 * 2. 도메인 보호: 외부 Context의 변경이 내부 도메인에 영향을 주지 않도록 방어
 * 3. MSA 준비: 향후 서비스 분리 시 API 경계에서 DTO 변환 역할
 *
 * 향후 확장:
 * - 검증 로직 추가 (예: ChatRoomId 유효성 검증)
 * - 변환 실패 시 예외 처리 전략
 * - MSA 환경에서 서비스 간 API 호출 시 DTO 변환
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
