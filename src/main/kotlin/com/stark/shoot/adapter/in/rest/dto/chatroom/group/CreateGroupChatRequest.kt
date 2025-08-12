package com.stark.shoot.adapter.`in`.rest.dto.chatroom.group

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

/**
 * 그룹 채팅방 생성 요청 DTO
 */
data class CreateGroupChatRequest(
    @field:NotBlank(message = "채팅방 제목은 필수입니다.")
    @field:Size(max = 50, message = "채팅방 제목은 50자 이하여야 합니다.")
    val title: String,
    
    @field:NotEmpty(message = "참여자는 최소 1명 이상이어야 합니다.")
    @field:Size(min = 1, max = 99, message = "참여자는 1명 이상 99명 이하여야 합니다.")
    val participants: Set<Long>
)