package com.stark.shoot.application.port.`in`.chatroom.group.command

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.group.CreateGroupChatRequest

/**
 * 그룹 채팅방 생성 명령
 */
data class CreateGroupChatCommand(
    val title: String,
    val participants: Set<Long>, // 참여자 사용자 ID 목록
    val createdBy: Long // 생성자 사용자 ID
) {
    init {
        require(title.isNotBlank()) { "채팅방 제목은 필수입니다." }
        require(title.length <= 50) { "채팅방 제목은 50자 이하여야 합니다." }
        require(participants.size >= 2) { "그룹 채팅방은 최소 2명의 참여자가 필요합니다." }
        require(participants.size <= 100) { "그룹 채팅방은 최대 100명까지 참여할 수 있습니다." }
        require(createdBy in participants) { "생성자는 참여자에 포함되어야 합니다." }
    }

    companion object {
        fun of(request: CreateGroupChatRequest, createdBy: Long): CreateGroupChatCommand {
            return CreateGroupChatCommand(
                title = request.title,
                participants = request.participants,
                createdBy = createdBy
            )
        }
    }
}