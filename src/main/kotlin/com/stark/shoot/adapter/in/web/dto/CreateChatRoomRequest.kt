package com.stark.shoot.adapter.`in`.web.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.bson.types.ObjectId

data class CreateChatRoomRequest @JsonCreator constructor(

    @JsonProperty("title")
    val title: String?,          // 채팅방 제목

    @JsonProperty("participants")
    val participants: MutableSet<ObjectId> // 참여자 ID 목록

)
