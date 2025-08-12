package com.stark.shoot.adapter.`in`.rest.dto.chatroom.group

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 그룹 채팅방 제목 변경 요청 DTO
 */
data class UpdateGroupTitleRequest(
    @field:NotBlank(message = "새 제목은 필수입니다.")
    @field:Size(max = 50, message = "제목은 50자 이하여야 합니다.")
    val newTitle: String
)