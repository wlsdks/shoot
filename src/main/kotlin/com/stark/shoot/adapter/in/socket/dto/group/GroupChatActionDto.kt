package com.stark.shoot.adapter.`in`.socket.dto.group

/**
 * 그룹 채팅방 액션 DTO
 */
data class GroupChatActionDto(
    val action: String, // "CREATE", "ADD_PARTICIPANT", "REMOVE_PARTICIPANT", "UPDATE_TITLE", "LEAVE"
    val chatRoomId: Long? = null,
    val title: String? = null,
    val participants: Set<Long>? = null,
    val participantsToAdd: Set<Long>? = null,
    val participantsToRemove: Set<Long>? = null,
    val newTitle: String? = null
) {
    enum class Action(val value: String) {
        CREATE("CREATE"),
        ADD_PARTICIPANT("ADD_PARTICIPANT"),
        REMOVE_PARTICIPANT("REMOVE_PARTICIPANT"),
        UPDATE_TITLE("UPDATE_TITLE"),
        LEAVE("LEAVE")
    }
}