package com.stark.shoot.application.service.event.message

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.MessageSentEvent
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener

@ApplicationEventListener
class MessageSentEventListener(
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val messageQueryPort: MessageQueryPort,
    private val webSocketMessageBroker: WebSocketMessageBroker
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 전송 이벤트 처리
     * - 채팅방 참여자들의 채팅방 목록을 실시간 업데이트
     * - 안읽은 메시지 개수, 마지막 메시지, 시간 등을 업데이트
     */
    @EventListener
    fun handleMessageSent(event: MessageSentEvent) {
        val message = event.message
        val roomId = message.roomId

        // 채팅방 정보 조회 (없으면 로그 경고 후 종료)
        val chatRoom = chatRoomQueryPort.findById(roomId) ?: run {
            logger.warn { "ChatRoom not found: ${roomId.value}" }
            return
        }

        // 각 참여자별로 채팅방 목록 업데이트 정보 전송
        chatRoom.participants.forEach { participantId ->
            try {
                // 해당 사용자의 안읽은 메시지 개수 계산
                val unreadCount = calculateUnreadCount(participantId, roomId, message.senderId)

                // 채팅방 목록 업데이트 데이터 생성
                val chatRoomUpdate = mapOf(
                    "roomId" to roomId.value,
                    "lastMessage" to mapOf(
                        "id" to message.id?.value,
                        "content" to message.content.text,
                        "senderId" to message.senderId.value,
                        "createdAt" to message.createdAt?.toString()
                    ),
                    "unreadCount" to unreadCount,
                    "lastActiveAt" to chatRoom.lastActiveAt.toString()
                )

                // 개별 사용자에게 채팅방 목록 업데이트 전송
                // todo: 일단은 전부 다 보내지만, 나중에 채팅방 내부에 있는 사용자는 안보내도 되도록 개선할 수 있음
                webSocketMessageBroker.sendMessage(
                    "/topic/user/${participantId.value}/chatrooms",
                    chatRoomUpdate
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to update chat room list for user ${participantId.value}" }
            }
        }
    }


    /**
     * 사용자의 특정 채팅방 안읽은 메시지 개수 계산
     */
    private fun calculateUnreadCount(
        userId: UserId,
        roomId: ChatRoomId,
        senderId: UserId
    ): Int {
        // 메시지 발송자는 안읽은 메시지가 0개 (자신이 보냈으니)
        if (userId == senderId) return 0

        return try {
            // MongoDB count 쿼리를 사용한 최적화된 안읽은 메시지 개수 조회
            messageQueryPort.countUnreadMessages(userId, roomId)
        } catch (e: Exception) {
            0
        }
    }

}
