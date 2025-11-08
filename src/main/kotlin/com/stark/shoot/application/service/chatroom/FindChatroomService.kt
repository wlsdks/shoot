package com.stark.shoot.application.service.chatroom

import com.stark.shoot.adapter.`in`.rest.dto.chatroom.ChatRoomResponse
import com.stark.shoot.infrastructure.mapper.ChatRoomResponseMapper
import com.stark.shoot.application.port.`in`.chatroom.FindChatRoomUseCase
import com.stark.shoot.application.port.`in`.chatroom.command.FindDirectChatCommand
import com.stark.shoot.application.port.`in`.chatroom.command.GetChatRoomsCommand
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.acl.*
import com.stark.shoot.domain.chatroom.service.ChatRoomDomainService
import com.stark.shoot.infrastructure.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@UseCase
class FindChatroomService(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val messageQueryPort: MessageQueryPort,
    private val chatRoomResponseMapper: ChatRoomResponseMapper,
    private val chatRoomDomainService: ChatRoomDomainService
) : FindChatRoomUseCase {

    /**
     * 사용자가 참여한 채팅방 목록을 조회합니다.
     *
     * N+1 쿼리 방지: 마지막 메시지를 배치로 조회하여 성능 최적화
     *
     * @param command 채팅방 목록 조회 커맨드
     * @return ChatRoomResponse 채팅방 목록
     */
    override fun getChatRoomsForUser(command: GetChatRoomsCommand): List<ChatRoomResponse> {
        val userId = command.userId

        // 사용자가 참여한 채팅방 목록을 조회합니다.
        val chatRooms = chatRoomQueryPort.findByParticipantId(userId)

        // 채팅방 정보 준비
        val titles = chatRoomDomainService.prepareChatRoomTitles(chatRooms, userId)

        // N+1 방지: 마지막 메시지를 배치로 조회
        val lastMessages = prepareLastMessagesBatch(chatRooms)

        val timestamps = chatRoomDomainService.prepareTimestamps(chatRooms)

        // 채팅방 목록을 ChatRoomResponse로 변환하여 반환합니다.
        return chatRoomResponseMapper.toResponseList(chatRooms, userId, titles, lastMessages, timestamps)
    }

    /**
     * 마지막 메시지들을 배치로 조회
     * N+1 쿼리 문제를 방지하기 위해 한 번의 쿼리로 모든 마지막 메시지 조회
     *
     * @param chatRooms 채팅방 목록
     * @return roomId -> 마지막 메시지 텍스트 맵
     */
    private fun prepareLastMessagesBatch(
        chatRooms: List<com.stark.shoot.domain.chatroom.ChatRoom>
    ): Map<Long, String> {
        // 1. 마지막 메시지 ID가 있는 채팅방만 필터링
        val roomsWithMessages = chatRooms.filter { it.lastMessageId != null }

        if (roomsWithMessages.isEmpty()) {
            return chatRooms.associate { room ->
                val roomId = room.id?.value ?: 0L
                roomId to "메시지가 없습니다."
            }
        }

        // 2. 모든 lastMessageId를 수집 (ACL 변환)
        val messageIds = roomsWithMessages.mapNotNull { room ->
            room.lastMessageId?.toChatMessageId()
        }

        // 3. 배치로 메시지 조회 (단 1번의 MongoDB 쿼리)
        val messages = messageQueryPort.findAllByIds(messageIds)
        val messagesById = messages.associateBy { it.id }

        // 4. 채팅방별 마지막 메시지 텍스트 맵 생성
        return chatRooms.associate { room ->
            val roomId = room.id?.value ?: 0L
            val lastMessageText = room.lastMessageId?.let { lastMsgId ->
                val chatMessageId = lastMsgId.toChatMessageId()
                val message = messagesById[chatMessageId]
                message?.let { formatMessageContent(it) } ?: "최근 메시지"
            } ?: "메시지가 없습니다."

            roomId to lastMessageText
        }
    }

    /**
     * 메시지 내용을 포맷팅
     */
    private fun formatMessageContent(message: com.stark.shoot.domain.chat.message.ChatMessage): String {
        return when {
            message.content.isDeleted -> "(삭제된 메시지)"
            message.content.text.isNotBlank() -> message.content.text
            message.content.attachments.isNotEmpty() -> {
                val attachment = message.content.attachments.first()
                when {
                    attachment.contentType.startsWith("image/") -> "\ud83d\uddbc\ufe0f 사진"
                    attachment.contentType.startsWith("video/") -> "\ud83c\udfa5 동영상"
                    attachment.contentType.startsWith("audio/") -> "\ud83c\udfa7 음성 메시지"
                    else -> "\ud83d\udcc4 ${attachment.filename}"
                }
            }
            else -> "최근 메시지"
        }
    }

    /**
     * 두 사용자 간의 1:1 채팅방을 찾습니다.
     *
     * @param command 직접 채팅 찾기 커맨드
     * @return 두 사용자 간의 1:1 채팅방 응답 객체, 없으면 null
     */
    override fun findDirectChatBetweenUsers(command: FindDirectChatCommand): ChatRoomResponse? {
        val userId1 = command.userId1
        val userId2 = command.userId2

        // 첫 번째 사용자가 참여한 채팅방 목록을 조회합니다.
        val chatRooms = chatRoomQueryPort.findByParticipantId(userId1)

        // 두 사용자 간의 1:1 채팅방을 찾습니다. (도메인 객체의 정적 메서드 사용)
        val directChatRoom = chatRoomDomainService.findDirectChatBetween(chatRooms, userId1, userId2)

        // 채팅방이 없으면 null 반환
        return directChatRoom?.let {
            val title = it.createChatRoomTitle(userId1)
            // todo: 마지막 메시지는 mongodb를 다녀와야하는데 이게 옳은지 검토가 필요.
            //  매번 이렇게 조회해야하나..? 그럼 새로운 메시지가 날라오면? sse로 업데이트 해야하나?
            val lastMessage = it.createLastMessageText()
            val timestamp = it.formatTimestamp()

            chatRoomResponseMapper.toResponse(it, userId1, title, lastMessage, timestamp)
        }
    }

}
