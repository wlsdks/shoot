package com.stark.shoot.adapter.`in`.rest.dto.chatroom.group

import com.stark.shoot.domain.chatroom.ChatRoom
import java.time.Instant

/**
 * 그룹 채팅방 응답 DTO
 */
data class GroupChatResponse(
    val id: Long,
    val title: String,
    val participants: Set<Long>,
    val participantCount: Int,
    val pinnedParticipants: Set<Long>,
    val lastActiveAt: Instant,
    val createdAt: Instant,
    val announcement: String? = null
) {
    companion object {
        fun from(chatRoom: ChatRoom): GroupChatResponse {
            return GroupChatResponse(
                id = chatRoom.id?.value ?: throw IllegalStateException("채팅방 ID가 없습니다."),
                title = chatRoom.title?.value ?: "제목 없음",
                participants = chatRoom.participants.map { it.value }.toSet(),
                participantCount = chatRoom.participants.size,
                pinnedParticipants = chatRoom.pinnedParticipants.map { it.value }.toSet(),
                lastActiveAt = chatRoom.lastActiveAt,
                createdAt = chatRoom.createdAt,
                announcement = chatRoom.announcement?.value
            )
        }
    }
}