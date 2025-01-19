package com.stark.shoot.adapter.`in`.web.dto

import org.bson.types.ObjectId

data class ManageParticipantRequest(
    val userId: ObjectId // 추가/제거할 참여자 ID
)
