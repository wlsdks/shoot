package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class ManageChatRoomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val findUserPort: FindUserPort
) : ManageChatRoomUseCase {

    /**
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return Boolean 참여자 추가 성공 여부
     */
    override fun addParticipant(
        roomId: Long,
        userId: Long
    ): Boolean {
        // 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(roomId)
            ?: throw IllegalArgumentException("채팅방을 찾을 수 없습니다: $roomId")

        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(userId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")

        }

        // 이미 참여 중인지 확인
        if (chatRoom.participants.contains(userId)) {
            return true // 이미 참여 중이면 true 반환
        }

        // 참여자 추가
        chatRoom.participants.add(userId)

        // 변경사항 저장
        saveChatRoomPort.save(chatRoom)

        return true
    }

    /**
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return Boolean 참여자 제거 성공 여부
     * @apiNote 채팅방 참여자 제거
     */
    override fun removeParticipant(
        roomId: Long,
        userId: Long
    ): Boolean {
        // 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(roomId)
            ?: throw IllegalArgumentException("채팅방을 찾을 수 없습니다.")

        // 참여자가 아닌 경우
        if (!chatRoom.participants.contains(userId)) {
            return false
        }

        // 참여자 제거
        chatRoom.participants.remove(userId)

        // 고정된 참여자 목록에서도 제거
        if (chatRoom.pinnedParticipants.contains(userId)) {
            chatRoom.pinnedParticipants.remove(userId)
        }

        // 변경사항 저장
        saveChatRoomPort.save(chatRoom)

        // 채팅방에 참여자가 없으면 추가 처리 가능 (예: 채팅방 삭제)
        if (chatRoom.participants.isEmpty()) {
            // 채팅방 삭제 로직 (필요시 구현)
        }

        return true
    }

    /**
     * 채팅방 공지사항을 업데이트합니다.
     *
     * @param roomId 채팅방 ID
     * @param announcement 공지사항 (null인 경우 공지사항 삭제)
     */
    override fun updateAnnouncement(
        roomId: Long,
        announcement: String?
    ) {
        // 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다: $roomId")

        // 공지사항 업데이트
        val updatedRoom = chatRoom.copy(announcement = announcement)

        // 변경사항 저장
        saveChatRoomPort.save(updatedRoom)
    }

}