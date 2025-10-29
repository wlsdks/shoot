package com.stark.shoot.application.service.message.listener

import com.stark.shoot.adapter.`in`.socket.WebSocketMessageBroker
import com.stark.shoot.domain.event.MessageDeletedEvent
import com.stark.shoot.domain.event.MessageEditedEvent
import com.stark.shoot.infrastructure.util.WebSocketResponseBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 메시지 이벤트에 대한 WebSocket 브로드캐스트 리스너
 *
 * Slack, Discord 등 업계 표준 패턴:
 * 1. 메시지를 DB에 영속화 (MongoDB atomic 작업)
 * 2. 저장 완료 확인
 * 3. WebSocket으로 브로드캐스트 (비동기 처리)
 *
 * 이 패턴을 사용하는 이유:
 * - 메시지 유실 방지: WebSocket 실패해도 메시지는 이미 MongoDB에 저장됨
 * - 작업 독립성: 외부 시스템(WebSocket) 실패가 저장 작업에 영향 없음
 * - 복구 가능성: 클라이언트가 재연결 시 MongoDB에서 메시지를 가져올 수 있음
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT)을 사용하여
 * MongoDB 저장 작업이 완료된 후에 WebSocket 전송을 수행합니다.
 *
 * 참고: MongoDB 단일 document 작업은 atomic하므로 트랜잭션이 없어도
 * AFTER_COMMIT은 여전히 "이벤트 발행 메서드 완료 후"를 의미합니다.
 */
@Component
class MessageEventWebSocketListener(
    private val webSocketMessageBroker: WebSocketMessageBroker
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 수정 이벤트 처리
     *
     * MongoDB 저장 완료 후에 실행되므로 메시지가 이미 DB에 안전하게 저장된 상태입니다.
     * WebSocket 전송이 실패하더라도 메시지는 유실되지 않습니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessageEdited(event: MessageEditedEvent) {
        try {
            // 채팅방의 모든 참여자에게 메시지 편집 알림
            webSocketMessageBroker.sendMessage(
                "/topic/message/edit/${event.roomId.value}",
                event.message
            )

            // 요청자에게 성공 응답 전송
            webSocketMessageBroker.sendMessage(
                "/queue/message/edit/response/${event.userId.value}",
                WebSocketResponseBuilder.success(event.message, "메시지가 수정되었습니다.")
            )

            logger.debug {
                "메시지 수정 WebSocket 전송 완료: messageId=${event.messageId.value}, " +
                        "roomId=${event.roomId.value}, userId=${event.userId.value}"
            }
        } catch (e: Exception) {
            // WebSocket 전송 실패는 로깅만 하고 예외를 전파하지 않음
            // 메시지는 이미 DB에 저장되어 있으므로 클라이언트가 재연결 시 동기화 가능
            logger.error(e) {
                "메시지 수정 WebSocket 전송 실패 (메시지는 이미 MongoDB에 저장됨): " +
                        "messageId=${event.messageId.value}, roomId=${event.roomId.value}"
            }
        }
    }

    /**
     * 메시지 삭제 이벤트 처리
     *
     * MongoDB 저장 완료 후에 실행되므로 메시지가 이미 DB에서 삭제 처리된 상태입니다.
     * WebSocket 전송이 실패하더라도 메시지 삭제 상태는 유지됩니다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessageDeleted(event: MessageDeletedEvent) {
        try {
            // 채팅방의 모든 참여자에게 메시지 삭제 알림
            webSocketMessageBroker.sendMessage(
                "/topic/message/delete/${event.roomId.value}",
                event.message
            )

            // 요청자에게 성공 응답 전송
            webSocketMessageBroker.sendMessage(
                "/queue/message/delete/response/${event.userId.value}",
                WebSocketResponseBuilder.success(event.message, "메시지가 삭제되었습니다.")
            )

            logger.debug {
                "메시지 삭제 WebSocket 전송 완료: messageId=${event.messageId.value}, " +
                        "roomId=${event.roomId.value}, userId=${event.userId.value}"
            }
        } catch (e: Exception) {
            // WebSocket 전송 실패는 로깅만 하고 예외를 전파하지 않음
            // 메시지 삭제는 이미 DB에 반영되어 있으므로 클라이언트가 재연결 시 동기화 가능
            logger.error(e) {
                "메시지 삭제 WebSocket 전송 실패 (삭제는 이미 MongoDB에 반영됨): " +
                        "messageId=${event.messageId.value}, roomId=${event.roomId.value}"
            }
        }
    }
}
