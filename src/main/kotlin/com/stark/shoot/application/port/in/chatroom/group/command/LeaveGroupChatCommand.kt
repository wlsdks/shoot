package com.stark.shoot.application.port.`in`.chatroom.group.command

/**
 * 그룹 채팅방 나가기 명령
 */
data class LeaveGroupChatCommand(
    val chatRoomId: Long,
    val userId: Long
) {
    init {
        require(chatRoomId > 0) { "채팅방 ID가 유효하지 않습니다." }
        require(userId > 0) { "사용자 ID가 유효하지 않습니다." }
    }
}