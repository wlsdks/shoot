package com.stark.shoot.adapter.`in`.web.dto

import com.fasterxml.jackson.annotation.JsonCreator

data class UpdateRoomSettingsRequest @JsonCreator constructor(
    val title: String?, // 변경할 제목 (선택)
    val notificationEnabled: Boolean? // 알림 설정 (선택)
)
