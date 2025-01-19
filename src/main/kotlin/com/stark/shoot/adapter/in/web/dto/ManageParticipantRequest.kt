package com.stark.shoot.adapter.`in`.web.dto

import com.fasterxml.jackson.annotation.JsonCreator

data class ManageParticipantRequest @JsonCreator constructor(
    val userId: String // 추가/제거할 참여자 ID
)
