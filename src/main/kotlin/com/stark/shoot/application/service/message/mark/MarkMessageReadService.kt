package com.stark.shoot.application.service.message.mark

import com.stark.shoot.adapter.`in`.web.socket.WebSocketMessageBroker
import com.stark.shoot.application.port.`in`.message.mark.MarkMessageReadUseCase
import com.stark.shoot.application.port.out.chatroom.LoadChatRoomPort
import com.stark.shoot.application.port.out.chatroom.ReadStatusPort
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.application.port.out.message.SaveMessagePort
import com.stark.shoot.domain.chat.event.ChatBulkReadEvent
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Transactional
@UseCase
class MarkMessageReadService(
    private val saveMessagePort: SaveMessagePort,
    private val loadChatRoomPort: LoadChatRoomPort,
    private val readStatusPort: ReadStatusPort,
    private val eventPublisher: EventPublisher,
    private val loadMessagePort: LoadMessagePort,
    private val redisTemplate: StringRedisTemplate,
    private val webSocketMessageBroker: WebSocketMessageBroker
) : MarkMessageReadUseCase {
    private val logger = KotlinLogging.logger {}

    /**
     * 단일 메시지를 읽음 상태로 표시합니다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @return 업데이트된 메시지
     */
    override fun markMessageAsRead(
        messageId: String,
        userId: Long
    ) {
        val chatMessage = loadMessagePort.findById(messageId.toObjectId())
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다. messageId=$messageId")

        // 이미 읽은 메시지인 경우 추가 작업 없이 반환
        if (chatMessage.readBy[userId] == true) {
            return
        }

        // 메시지 읽음 상태 업데이트
        chatMessage.readBy[userId] = true
        val updatedMessage = saveMessagePort.save(chatMessage)
        val roomId = chatMessage.roomId

        // 병렬로 비동기 작업 처리 (트랜잭션 외부에서 실행되어야 하는 작업들)
        try {
            // 1. 채팅방 사용자의 마지막 읽은 메시지 ID 업데이트
            readStatusPort.updateLastReadMessageId(roomId, userId, messageId)

            // 2. 읽지 않은 메시지 수 업데이트 (Redis 활용) - 원자적 연산 사용
            val unreadKey = "unread:$userId"
            val roomIdStr = roomId.toString()

            // Lua 스크립트로 원자적 감소 처리
            val script = """
                local current = tonumber(redis.call('hget', KEYS[1], ARGV[1]) or '0')
                if current > 0 then
                    return redis.call('hincrby', KEYS[1], ARGV[1], -1)
                end
                return current
            """

            redisTemplate.execute<Long> { connection ->
                connection.scriptingCommands().eval(
                    script.toByteArray(),
                    org.springframework.data.redis.connection.ReturnType.INTEGER,
                    1,
                    unreadKey.toByteArray(),
                    roomIdStr.toByteArray()
                )
            }

            // 3. 채팅방의 모든 참여자에게 읽은 상태 알림 (WebSocket)
            webSocketMessageBroker.sendMessage(
                "/topic/read/$roomId",
                mapOf(
                    "messageId" to updatedMessage.id,
                    "userId" to updatedMessage.senderId,
                    "readBy" to updatedMessage.readBy
                )
            )

            // 4. 업데이트된 메시지 WebSocket 전송
            webSocketMessageBroker.sendMessage(
                destination = "/topic/messages/$roomId",
                payload = updatedMessage
            )
        } catch (e: Exception) {
            // 비동기 작업 실패 시 로깅만 하고 예외는 전파하지 않음 (메인 트랜잭션은 이미 완료됨)
            logger.error(e) { "메시지 읽음 처리 후속 작업 실패: messageId=$messageId, userId=$userId" }
        }
    }

    /**
     * 채팅방의 모든 메시지를 읽음 상태로 표시합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param requestId 프론트에서 생성한 요청 ID (중복 요청 방지용, 선택적이며 유저나 채팅방id와는 전혀 관계 없음)
     */
    override fun markAllMessagesAsRead(
        roomId: Long,
        userId: Long,
        requestId: Long?
    ) {
        // 중복 요청 체크 (Redis 캐시 활용)
        if (requestId != null) {
            val key = "read_operation:$roomId:$userId:$requestId"

            // 중복 요청 확인 및 설정을 원자적으로 수행 (SETNX 패턴)
            val isFirstRequest = redisTemplate.opsForValue().setIfAbsent(key, "1", 30, TimeUnit.SECONDS)

            if (isFirstRequest != true) {
                logger.info { "중복 읽음 처리 요청 감지 (requestId=$requestId)" }
                return
            }

            // 30초 만료 시간은 일반적인 네트워크 지연 및 클라이언트 재시도를 고려한 값
            // 클라이언트는 새로운 읽음 요청마다 새로운 requestId를 생성해야 함
        }

        // 채팅방 조회 및 사용자 참여 확인
        val chatRoom = loadChatRoomPort.findById(roomId)
            ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다. roomId=$roomId")

        if (!chatRoom.participants.contains(userId)) {
            throw IllegalArgumentException("참여하지 않은 채팅방입니다. userId=$userId, roomId=$roomId")
        }

        // 읽지 않은 메시지 조회 (내가 보낸 메시지 제외)
        val unreadMessages = loadMessagePort.findUnreadByRoomId(roomId, userId)
            .filter { it.senderId != userId }

        // 읽지 않은 메시지가 없으면 불필요한 작업 방지
        if (unreadMessages.isEmpty()) {
            logger.info { "읽지 않은 메시지가 없습니다. roomId=$roomId, userId=$userId" }
            return
        }

        // 모든 메시지 읽음 처리
        val updatedMessageIds = processUnreadMessages(unreadMessages, userId)

        // 마지막으로 읽은 메시지 ID 업데이트
        val lastMessage = unreadMessages.maxByOrNull { it.createdAt ?: java.time.Instant.MIN }?.id
        if (lastMessage != null) {
            readStatusPort.updateLastReadMessageId(roomId, userId, lastMessage)
        }

        // Redis와 이벤트 업데이트 (읽지 않은 메시지 수 0으로 설정)
        // 원자적 연산으로 효율적으로 처리
        val unreadKey = "unread:$userId"
        val roomIdStr = roomId.toString()
        redisTemplate.execute<Boolean> { connection ->
            connection.hashCommands().hSet(
                unreadKey.toByteArray(),
                roomIdStr.toByteArray(),
                "0".toByteArray()
            )
        }

        // 채팅방의 마지막 메시지 내용 조회
        val lastMessageText = chatRoom.lastMessageId?.let {
            // 실제 구현시 메시지 내용 조회 로직 추가
            "최근 메시지"
        } ?: "메시지가 없습니다"

        if (updatedMessageIds.isNotEmpty()) {
            // WebSocket을 통해 읽음 완료처리된 메시지 id를 실시간 알림
            sendBulkReadNotification(roomId, updatedMessageIds, userId)

            // 읽지 않은 메시지 수 업데이트 이벤트 발행
            publishEvent(roomId, userId, lastMessageText)
        }

        logger.info { "채팅방 모든 메시지 읽음 처리 완료: roomId=$roomId, userId=$userId, 메시지 수=${updatedMessageIds.size}" }
    }

    /**
     * 읽지 않은 메시지들을 일괄 처리합니다.
     *
     * @param unreadMessages 읽지 않은 메시지 목록
     * @param userId 사용자 ID
     * @return 업데이트된 메시지 ID 목록
     */
    private fun processUnreadMessages(
        unreadMessages: List<ChatMessage>,
        userId: Long
    ): List<String> {
        // 모든 메시지를 한 번에 업데이트하도록 최적화
        unreadMessages.forEach { message ->
            message.readBy[userId] = true
        }

        // 일괄 저장으로 DB 쿼리 최소화
        val updatedMessages = saveMessagePort.saveAll(unreadMessages)

        // 업데이트된 메시지 ID 목록 반환
        return updatedMessages.mapNotNull { it.id }
    }


    /**
     * 채팅방의 모든 참여자에게 읽음 상태 업데이트를 WebSocket을 통해 알립니다.
     *
     * @param roomId 채팅방 ID
     * @param messageIds 읽은 메시지 ID 목록
     * @param userId 사용자 ID
     */
    private fun sendBulkReadNotification(
        roomId: Long,
        messageIds: List<String>,
        userId: Long
    ) {
        webSocketMessageBroker.sendMessage(
            "/topic/read-bulk/$roomId",
            ChatBulkReadEvent(roomId, messageIds, userId)
        )
    }

    /**
     * 채팅방의 모든 참여자에게 읽음 상태 업데이트 이벤트를 발행합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param lastMessage 마지막 메시지 내용
     */
    private fun publishEvent(
        roomId: Long,
        userId: Long,
        lastMessage: String
    ) {
        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = roomId,
                unreadCounts = mapOf(userId to 0),
                lastMessage = lastMessage
            )
        )
    }

}
