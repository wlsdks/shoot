package com.stark.shoot.adapter.`in`.rest.dto.chatroom.group

import jakarta.validation.constraints.NotEmpty

/**
 * 참여자 관리 요청 DTO
 */
data class ManageParticipantsRequest(
    val participantsToAdd: Set<Long> = emptySet(),
    val participantsToRemove: Set<Long> = emptySet()
) {
    init {
        require(participantsToAdd.isNotEmpty() || participantsToRemove.isNotEmpty()) {
            "추가하거나 제거할 참여자가 있어야 합니다."
        }
        require((participantsToAdd intersect participantsToRemove).isEmpty()) {
            "동일한 사용자를 동시에 추가하고 제거할 수 없습니다."
        }
    }
}