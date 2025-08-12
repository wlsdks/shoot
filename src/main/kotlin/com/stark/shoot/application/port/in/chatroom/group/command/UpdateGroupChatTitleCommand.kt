package com.stark.shoot.application.port.`in`.chatroom.group.command

/**
 * 그룹 채팅방 제목 변경 명령
 */
data class UpdateGroupChatTitleCommand(
    val chatRoomId: Long,
    val newTitle: String,
    val updatedBy: Long
) {
    init {
        require(chatRoomId > 0) { "채팅방 ID가 유효하지 않습니다." }
        require(newTitle.isNotBlank()) { "새 제목은 필수입니다." }
        require(newTitle.length <= 50) { "제목은 50자 이하여야 합니다." }
        require(updatedBy > 0) { "수정자 ID가 유효하지 않습니다." }
    }
}