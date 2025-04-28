package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.DeleteChatRoomPort
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class ManageChatRoomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val deleteChatRoomPort: DeleteChatRoomPort,
    private val findUserPort: FindUserPort
) : ManageChatRoomUseCase {

    private val logger = KotlinLogging.logger {}

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

        // 참여자 추가 (도메인 객체의 메서드 사용)
        // 도메인 메서드 내부에서 이미 참여 중인지 확인하는 로직 처리
        val updatedChatRoom = chatRoom.addParticipant(userId)

        // 변경사항 저장
        saveChatRoomPort.save(updatedChatRoom)

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

        // 참여자 제거 (도메인 객체의 메서드 사용)
        // 도메인 메서드 내부에서 참여자가 아닌 경우 처리
        val updatedChatRoom = chatRoom.removeParticipant(userId)

        // 참여자가 아니었으면 변경이 없으므로 원래 객체와 동일
        if (updatedChatRoom === chatRoom) {
            return false
        }

        // 변경사항 저장
        saveChatRoomPort.save(updatedChatRoom)

        // 채팅방에 참여자가 없으면 채팅방 삭제
        if (updatedChatRoom.isEmpty()) {
            logger.info { "빈 채팅방 삭제 시도: roomId=$roomId" }
            val deleted = deleteChatRoomPort.deleteById(roomId)
            if (deleted) {
                logger.info { "빈 채팅방 삭제 완료: roomId=$roomId" }
            } else {
                logger.warn { "빈 채팅방 삭제 실패: roomId=$roomId" }
            }
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

        // 공지사항 업데이트 (도메인 객체의 메서드 사용)
        val updatedRoom = chatRoom.updateAnnouncement(announcement)

        // 변경사항 저장
        saveChatRoomPort.save(updatedRoom)
    }

}
