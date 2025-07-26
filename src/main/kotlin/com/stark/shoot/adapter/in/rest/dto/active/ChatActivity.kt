package com.stark.shoot.adapter.`in`.rest.dto.active

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

/**
 * 채팅방 활동 상태 DTO
 *
 * 사용자의 채팅방 활동 상태를 나타내는 데이터 객체입니다.
 * 사용자가 채팅방에 입장하거나 퇴장할 때 이 객체를 통해 상태를 업데이트합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ChatActivity(
    @field:NotNull(message = "사용자 ID는 필수입니다")
    @JsonProperty("userId")
    val userId: Long,

    @field:NotNull(message = "채팅방 ID는 필수입니다")
    @JsonProperty("roomId")
    val roomId: Long,

    @field:NotNull(message = "활동 상태는 필수입니다")
    @JsonProperty("active")
    val active: Boolean
)
