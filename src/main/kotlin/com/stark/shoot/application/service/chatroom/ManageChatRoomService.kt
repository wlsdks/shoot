package com.stark.shoot.application.service.chatroom

import com.stark.shoot.application.port.`in`.chatroom.ManageChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.AddParticipantCommand
import com.stark.shoot.application.port.`in`.chatroom.command.RemoveParticipantCommand
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateAnnouncementCommand
import com.stark.shoot.application.port.`in`.chatroom.command.UpdateTitleCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomCommandPort
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.user.UserQueryPort
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.service.ChatRoomParticipantDomainService
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import org.springframework.transaction.annotation.Transactional

@Transactional
@UseCase
class ManageChatRoomService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val chatRoomCommandPort: ChatRoomCommandPort,
    private val userQueryPort: UserQueryPort,
    private val participantDomainService: ChatRoomParticipantDomainService
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
        roomId: ChatRoomId,
        errorMessage: String = "채팅방을 찾을 수 없습니다: $roomId",
        operation: (ChatRoom) -> Pair<ChatRoom, T>
    ): T {
        // 채팅방 조회
        val chatRoom = chatRoomQueryPort.findById(roomId)
            ?: throw ResourceNotFoundException(errorMessage)

        // 작업 수행
        val (updatedRoom, result) = operation(chatRoom)

        // 변경사항 저장
        chatRoomCommandPort.save(updatedRoom)

        return result
    }

    /**
     * 채팅방에 참여자를 추가합니다.
     *
     * @param command 참여자 추가 커맨드
     * @return 참여자 추가 성공 여부
     */
    override fun addParticipant(command: AddParticipantCommand): Boolean {
        val roomId = command.roomId
        val userId = command.userId

        // 사용자 존재 여부 확인
        if (!userQueryPort.existsById(userId)) {
            throw ResourceNotFoundException("사용자를 찾을 수 없습니다: $userId")
        }

        return withChatRoom(roomId, "채팅방을 찾을 수 없습니다: $roomId") { chatRoom ->
            // 도메인 서비스에 위임하여 참여자 추가
            val updatedChatRoom = participantDomainService.addParticipant(chatRoom, userId)

            // 결과 반환 (업데이트된 채팅방과 성공 여부)
            Pair(updatedChatRoom, true)
        }
    }

    /**
     * 채팅방에서 참여자를 제거합니다.
     *
     * @param command 참여자 제거 커맨드
     * @return 참여자 제거 성공 여부
     */
    override fun removeParticipant(command: RemoveParticipantCommand): Boolean {
        val roomId = command.roomId
        val userId = command.userId

        return withChatRoom(roomId, "채팅방을 찾을 수 없습니다.") { chatRoom ->
            // 도메인 서비스에 위임하여 참여자 제거 및 삭제 필요 여부 확인
            val result = participantDomainService.removeParticipant(chatRoom, userId)

            // 참여자가 아니었으면 변경이 없으므로 원래 객체와 동일
            if (result.chatRoom === chatRoom) {
                return@withChatRoom Pair(chatRoom, false)
            }

            // 채팅방이 삭제 대상이면 삭제 처리
            if (result.shouldDeleteRoom) {
                chatRoomCommandPort.deleteById(roomId)
            }

            // 결과 반환 (업데이트된 채팅방과 성공 여부)
            Pair(result.chatRoom, true)
        }
    }

    /**
     * 채팅방 공지사항을 업데이트합니다.
     *
     * @param command 공지사항 업데이트 커맨드
     */
    override fun updateAnnouncement(command: UpdateAnnouncementCommand) {
        val roomId = command.roomId
        val announcement = command.announcement

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
     * @param command 제목 업데이트 커맨드
     * @return 업데이트 성공 여부
     */
    override fun updateTitle(command: UpdateTitleCommand): Boolean {
        val roomId = command.roomId
        val title = command.title

        return withChatRoom(roomId) { chatRoom ->
            // 제목 업데이트 (도메인 객체의 update 메서드 사용)
            val updatedRoom = chatRoom.update(title = title)

            // 결과 반환 (업데이트된 채팅방과 성공 여부)
            Pair(updatedRoom, true)
        }
    }

}
