package com.stark.shoot.adapter.`in`.web.dto

import org.bson.types.ObjectId

data class CreateChatRoomRequest(

    val title: String?,          // 채팅방 제목
    val participants: MutableSet<ObjectId> // 참여자 ID 목록

)
