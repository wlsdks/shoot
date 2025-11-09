package com.stark.shoot.application.service.message.mark

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.vo.ChatRoomId
import com.stark.shoot.domain.shared.event.MessageBulkReadEvent
import com.stark.shoot.domain.shared.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 메시지 읽음 처리 관련 WebSocket 알림을 담당하는 서비스
 *
 * MessageReadService의 알림 책임을 분리하여 단일 책임 원칙을 준수합니다.
 */
@Component
class MessageReadNotificationService(
    private val webSocketMessageBroker: WebSocketMessageBroker
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 단일 메시지 읽음 상태 알림을 전송합니다.
     *
     * @param roomId 채팅방 ID
     * @param messageId 읽음 처리된 메시지 ID
     * @param userId 읽음 처리한 사용자 ID
     */
    fun sendSingleReadNotification(
        roomId: ChatRoomId,
        messageId: MessageId,
        userId: UserId
    ) {
        try {
            webSocketMessageBroker.sendMessage(
                "/topic/read/${roomId.value}",
                mapOf(
                    "messageId" to messageId.value,
                    "userId" to userId.value
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "단일 메시지 읽음 알림 전송 실패: roomId=$roomId, messageId=$messageId, userId=$userId" }
            // WebSocket 알림 실패는 전체 프로세스를 중단시킬 만큼 치명적이지 않음
        }
    }

    /**
     * 채팅방의 모든 참여자에게 일괄 읽음 상태 업데이트를 WebSocket을 통해 알립니다.
     *
     * @param roomId 채팅방 ID
     * @param messageIds 읽음 처리된 메시지 ID 목록
     * @param userId 읽음 처리한 사용자 ID
     */
    fun sendBulkReadNotification(
        roomId: ChatRoomId,
        messageIds: List<MessageId>,
        userId: UserId
    ) {
        if (messageIds.isEmpty()) {
            logger.debug { "알림 대상 메시지가 없습니다: roomId=${roomId.value}, userId=${userId.value}" }
            return
        }

        try {
            webSocketMessageBroker.sendMessage(
                "/topic/read-bulk/${roomId.value}",
                MessageBulkReadEvent.create(roomId, messageIds, userId)
            )
            logger.debug { "일괄 읽음 알림 전송 완료: roomId=${roomId.value}, userId=${userId.value}, 메시지 수=${messageIds.size}" }
        } catch (e: Exception) {
            logger.error(e) { "일괄 읽음 알림 전송 실패: roomId=$roomId, userId=$userId" }
            // WebSocket 알림 실패는 중요하지만 전체 프로세스를 중단시킬 만큼 치명적이지 않음
        }
    }
}
