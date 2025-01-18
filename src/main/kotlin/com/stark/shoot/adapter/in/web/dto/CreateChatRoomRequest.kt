package com.stark.shoot.adapter.`in`.web.dto

data class CreateChatRoomRequest(
    val title: String?,          // 채팅방 제목
    val participants: MutableSet<String> // 참여자 ID 목록
)
