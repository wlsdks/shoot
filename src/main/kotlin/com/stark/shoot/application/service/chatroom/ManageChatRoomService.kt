package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.out.chatroom.DeleteChatRoomPort
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.SaveChatRoomPort
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.room.ChatRoom
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class ManageChatRoomService(
    private val loadChatRoomPort: LoadChatRoomPort,
    private val saveChatRoomPort: SaveChatRoomPort,
    private val deleteChatRoomPort: DeleteChatRoomPort,
    private val findUserPort: FindUserPort
) : ManageChatRoomUseCase {

    /**
     * 채팅방을 로드하고 작업을 수행한 후 저장하는 공통 메서드
     *
     * @param roomId 채팅방 ID
     * @param errorMessage 채팅방을 찾을 수 없을 때 표시할 오류 메시지
     * @param operation 채팅방에 수행할 작업 (함수)
     * @return 작업 결과
     */
    private fun <T> withChatRoom(
        roomId: Long,
        errorMessage: String = "채팅방을 찾을 수 없습니다: $roomId",
        operation: (ChatRoom) -> Pair<ChatRoom, T>
    ): T {
        // 채팅방 조회
        val chatRoom = loadChatRoomPort.findById(roomId)
            ?: throw ResourceNotFoundException(errorMessage)

        // 작업 수행
        val (updatedRoom, result) = operation(chatRoom)

        // 변경사항 저장
        saveChatRoomPort.save(updatedRoom)

        return result
    }

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
        // 사용자 존재 여부 확인
        if (!findUserPort.existsById(userId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")
        }

        return withChatRoom(roomId, "채팅방을 찾을 수 없습니다: $roomId") { chatRoom ->
            // 참여자 추가 (도메인 객체의 메서드 사용)
            // 도메인 메서드 내부에서 이미 참여 중인지 확인하는 로직 처리
            val updatedChatRoom = chatRoom.addParticipant(userId)

            // 결과 반환 (업데이트된 채팅방과 성공 여부)
            Pair(updatedChatRoom, true)
        }
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
        return withChatRoom(roomId, "채팅방을 찾을 수 없습니다.") { chatRoom ->
            // 참여자 제거 (도메인 객체의 메서드 사용)
            // 도메인 메서드 내부에서 참여자가 아닌 경우 처리
            val updatedChatRoom = chatRoom.removeParticipant(userId)

            // 참여자가 아니었으면 변경이 없으므로 원래 객체와 동일
            if (updatedChatRoom === chatRoom) {
                return@withChatRoom Pair(chatRoom, false)
            }

            // 채팅방이 삭제되어야 하는지 도메인 모델에서 판단
            if (updatedChatRoom.shouldBeDeleted()) {
                // 저장 후 삭제 (withChatRoom 메서드가 저장을 처리함)
                deleteChatRoomPort.deleteById(roomId)
            }

            // 결과 반환 (업데이트된 채팅방과 성공 여부)
            Pair(updatedChatRoom, true)
        }
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
        withChatRoom(roomId) { chatRoom ->
            // 공지사항 업데이트 (도메인 객체의 메서드 사용)
            val updatedRoom = chatRoom.updateAnnouncement(announcement)

            // 결과 반환 (업데이트된 채팅방과 Unit 결과)
            Pair(updatedRoom, Unit)
        }
    }

    /**
     * 채팅방 제목을 업데이트합니다.
     *
     * @param roomId 채팅방 ID
     * @param title 새로운 채팅방 제목
     * @return 업데이트 성공 여부
     */
    override fun updateTitle(
        roomId: Long,
        title: String
    ): Boolean {
        return withChatRoom(roomId) { chatRoom ->
            // 제목 업데이트 (도메인 객체의 update 메서드 사용)
            val updatedRoom = chatRoom.update(title = title)

            // 결과 반환 (업데이트된 채팅방과 성공 여부)
            Pair(updatedRoom, true)
        }
    }

}
