package com.stark.shoot.application.port.`in`.chatroom.group.command

/**
 * 그룹 채팅방 참여자 관리 명령
 */
data class ManageGroupParticipantsCommand(
    val chatRoomId: Long,
    val participantsToAdd: Set<Long> = emptySet(),
    val participantsToRemove: Set<Long> = emptySet(),
    val managedBy: Long
) {
    init {
        require(chatRoomId > 0) { "채팅방 ID가 유효하지 않습니다." }
        require(participantsToAdd.isNotEmpty() || participantsToRemove.isNotEmpty()) { 
            "추가하거나 제거할 참여자가 있어야 합니다." 
        }
        require((participantsToAdd intersect participantsToRemove).isEmpty()) { 
            "동일한 사용자를 동시에 추가하고 제거할 수 없습니다." 
        }
    }

    companion object {
        fun addParticipants(chatRoomId: Long, participants: Set<Long>, managedBy: Long): ManageGroupParticipantsCommand {
            return ManageGroupParticipantsCommand(
                chatRoomId = chatRoomId,
                participantsToAdd = participants,
                managedBy = managedBy
            )
        }

        fun removeParticipants(chatRoomId: Long, participants: Set<Long>, managedBy: Long): ManageGroupParticipantsCommand {
            return ManageGroupParticipantsCommand(
                chatRoomId = chatRoomId,
                participantsToRemove = participants,
                managedBy = managedBy
            )
        }
    }
}