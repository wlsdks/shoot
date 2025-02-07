package com.stark.shoot.adapter.`in`.web.dto.message

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true) // 정의되지 않은 필드는 무시
data class ChatMessageRequest(
    val roomId: String,       // 채팅방 ID
    val senderId: String,     // 보낸 사람 ID
    val content: MessageContentRequest  // 메시지 내용
) {

}
