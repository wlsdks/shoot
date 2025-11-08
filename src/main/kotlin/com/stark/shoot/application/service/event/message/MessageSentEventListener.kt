package com.stark.shoot.application.service.event.message

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.acl.*
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.event.MessageSentEvent
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.domain.shared.event.EventVersion
import com.stark.shoot.domain.shared.event.EventVersionValidator
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

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
     * 트랜잭션 커밋 후에 실행되어 데이터 일관성을 보장합니다.
     *
     * N+1 쿼리 최적화: 배치 쿼리로 한번에 모든 참여자의 unread count 조회
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessageSent(event: MessageSentEvent) {
        // Event Version 검증
        EventVersionValidator.checkAndLog(event, EventVersion.MESSAGE_SENT_V1, "MessageSentEventListener")

        // DDD 개선: 이벤트에서 직접 필드 사용
        val roomId = event.roomId

        // 채팅방 정보 조회 (없으면 로그 경고 후 종료)
        val chatRoom = chatRoomQueryPort.findById(roomId.toChatRoom()) ?: run {
            logger.warn { "ChatRoom not found: ${roomId.value}" }
            return
        }

        // 배치 쿼리로 모든 참여자의 unread count를 한번에 조회 (N+1 문제 해결)
        val unreadCounts = try {
            messageQueryPort.countUnreadMessagesBatch(chatRoom.participants, roomId.toChatRoom())
        } catch (e: Exception) {
            logger.warn(e) { "Failed to batch count unread messages for room $roomId" }
            // 실패 시 모든 참여자 0으로 초기화
            chatRoom.participants.associateWith { 0 }
        }

        // 각 참여자별로 채팅방 목록 업데이트 정보 전송
        chatRoom.participants.forEach { participantId ->
            try {
                // 발신자는 항상 0, 나머지는 배치 쿼리 결과 사용
                val unreadCount = if (participantId == event.senderId) 0
                                  else unreadCounts[participantId] ?: 0

                // 채팅방 목록 업데이트 데이터 생성
                val chatRoomUpdate = mapOf(
                    "roomId" to roomId.value,
                    "lastMessage" to mapOf(
                        "id" to event.messageId.value,
                        "content" to event.content,
                        "senderId" to event.senderId.value,
                        "createdAt" to event.createdAt.toString()
                    ),
                    "unreadCount" to unreadCount,
                    "lastActiveAt" to chatRoom.lastActiveAt.toString()
                )

                // 개별 사용자에게 채팅방 목록 업데이트 전송
                // 비동기 전송으로 성능 최적화
                webSocketMessageBroker.sendMessage(
                    "/topic/user/${participantId.value}/chatrooms",
                    chatRoomUpdate,
                    retryCount = 2 // 채팅방 목록 업데이트는 재시도 횟수 축소
                ).whenComplete { success, throwable ->
                    if (!success || throwable != null) {
                        logger.warn { "Failed to send chatroom update to user ${participantId.value}: ${throwable?.message}" }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to update chat room list for user ${participantId.value}" }
            }
        }
    }

}
