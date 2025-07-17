package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.message.impl.ChatRoomLoadFilter.Companion.CHAT_ROOM_CONTEXT_KEY
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chatroom.ChatRoom
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.event.ChatRoomUpdateEvent
import com.stark.shoot.domain.user.vo.UserId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class EventPublishFilter(
    private val eventPublisher: EventPublisher,
    private val redisTemplate: StringRedisTemplate
) : MessageProcessingFilter {

    private val logger = KotlinLogging.logger {}

    override fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 컨텍스트에서 채팅방 정보 가져오기
        val chatRoom = chain.getFromContext<ChatRoom>(CHAT_ROOM_CONTEXT_KEY)
            ?: return chain.proceed(message) // 채팅방 정보가 없으면 다음 필터로 진행

        // 채팅방 ID가 null인 경우 처리
        val roomId = chatRoom.id ?: run {
            logger.warn { "채팅방 ID가 null입니다. 이벤트를 발행하지 않고 다음 필터로 진행합니다." }
            return chain.proceed(message)
        }

        // 각 참여자의 읽지 않은 메시지 수 계산
        val unreadCounts = calculateUnreadCounts(message, chatRoom, roomId)

        // 채팅방 업데이트 이벤트 발행
        val updates = unreadCounts.mapValues { (_, count) ->
            ChatRoomUpdateEvent.Update(
                unreadCount = count,
                lastMessage = message.content.text
            )
        }
        eventPublisher.publish(
            ChatRoomUpdateEvent.create(
                roomId = roomId,
                updates = updates
            )
        )

        return chain.proceed(message)
    }

    /**
     * 각 참여자의 읽지 않은 메시지 수를 계산하고 Redis를 업데이트합니다.
     */
    private fun calculateUnreadCounts(
        message: ChatMessage,
        chatRoom: ChatRoom,
        roomId: ChatRoomId
    ): Map<UserId, Int> {
        val unreadCounts = mutableMapOf<UserId, Int>()
        val operations = redisTemplate.opsForHash<String, String>()

        // 메시지를 읽지 않은 참여자 식별 및 처리
        chatRoom.participants.forEach { userId ->
            if (message.readBy[userId] != true) {
                try {
                    // Redis에서 현재 읽지 않은 메시지 수 조회
                    val currentUnreadCount = operations
                        .get("unread:${userId.value}", roomId.value.toString())?.toIntOrNull() ?: 0

                    // 읽지 않은 메시지 수 증가
                    val newUnreadCount = currentUnreadCount + 1

                    // Redis 업데이트
                    operations.put("unread:${userId.value}", roomId.value.toString(), newUnreadCount.toString())

                    // 이벤트에 포함할 unreadCounts 맵 업데이트
                    unreadCounts[userId] = newUnreadCount
                } catch (e: Exception) {
                    logger.error(e) { "Redis 읽지 않은 메시지 수 업데이트 실패: roomId=$roomId, userId=${userId.value}" }
                    // 오류 발생 시 기본값 사용
                    unreadCounts[userId] = 1
                }
            } else {
                // 이미 읽은 사용자는 0으로 설정
                unreadCounts[userId] = 0
            }
        }

        return unreadCounts
    }
}