package com.stark.shoot.application.port.`in`.chatroom

import com.stark.shoot.application.port.`in`.chatroom.command.AddParticipantCommand
import com.stark.shoot.application.port.`in`.chatroom.command.RemoveParticipantCommand
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateAnnouncementCommand
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateTitleCommand

interface ManageChatRoomUseCase {
    /**
     * 채팅방에 참여자를 추가합니다.
     *
     * @param command 참여자 추가 커맨드
     * @return 참여자 추가 성공 여부
     */
    fun addParticipant(command: AddParticipantCommand): Boolean

    /**
     * 채팅방에서 참여자를 제거합니다.
     *
     * @param command 참여자 제거 커맨드
     * @return 참여자 제거 성공 여부
     */
    fun removeParticipant(command: RemoveParticipantCommand): Boolean

    /**
     * 채팅방 공지사항을 업데이트합니다.
     *
     * @param command 공지사항 업데이트 커맨드
     */
    fun updateAnnouncement(command: UpdateAnnouncementCommand)

    /**
     * 채팅방 제목을 업데이트합니다.
     *
     * @param command 제목 업데이트 커맨드
     * @return 제목 업데이트 성공 여부
     */
    fun updateTitle(command: UpdateTitleCommand): Boolean
}
