package com.stark.shoot.application.filter.message.impl

import com.stark.shoot.application.filter.common.MessageProcessingFilter
import com.stark.shoot.application.filter.message.chain.MessageProcessingChain
import com.stark.shoot.application.filter.message.impl.ChatRoomLoadFilter.Companion.CHAT_ROOM_CONTEXT_KEY
import com.stark.shoot.application.port.out.event.EventPublisher
import com.stark.shoot.domain.chat.event.ChatUnreadCountUpdatedEvent
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.room.ChatRoom
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class EventPublishFilter(
    private val eventPublisher: EventPublisher,
    private val redisTemplate: StringRedisTemplate
) : MessageProcessingFilter {

    private val logger = KotlinLogging.logger {}

    override suspend fun process(
        message: ChatMessage,
        chain: MessageProcessingChain
    ): ChatMessage {
        // 컨텍스트에서 채팅방 정보 가져오기
        val chatRoom = chain.getFromContext<ChatRoom>(CHAT_ROOM_CONTEXT_KEY)
            ?: return chain.proceed(message) // 채팅방 정보가 없으면 다음 필터로 진행

        // 각 참여자의 읽지 않은 메시지 수 계산
        val unreadCounts = mutableMapOf<Long, Int>()

        // 메시지를 읽지 않은 참여자 식별
        chatRoom.participants.forEach { userId ->
            // 메시지를 읽지 않은 경우에만 unreadCounts에 추가
            if (message.readBy[userId] != true) {
                try {
                    // Redis에서 현재 읽지 않은 메시지 수 조회
                    val currentUnreadCount = redisTemplate.opsForHash<String, String>()
                        .get("unread:$userId", chatRoom.id.toString())?.toIntOrNull() ?: 0

                    // 읽지 않은 메시지 수 증가
                    val newUnreadCount = currentUnreadCount + 1

                    // Redis 업데이트
                    redisTemplate.opsForHash<String, String>()
                        .put("unread:$userId", chatRoom.id.toString(), newUnreadCount.toString())

                    // 이벤트에 포함할 unreadCounts 맵 업데이트
                    unreadCounts[userId] = newUnreadCount
                } catch (e: Exception) {
                    logger.error(e) { "Redis 읽지 않은 메시지 수 업데이트 실패: roomId=${chatRoom.id}, userId=$userId" }
                    // 오류 발생 시 기본값 사용
                    unreadCounts[userId] = 1
                }
            } else {
                // 이미 읽은 사용자는 0으로 설정
                unreadCounts[userId] = 0
            }
        }

        // 읽지 않은 메시지 수 이벤트 발행
        eventPublisher.publish(
            ChatUnreadCountUpdatedEvent(
                roomId = chatRoom.id!!,
                unreadCounts = unreadCounts,
                lastMessage = message.content.text
            )
        )

        return chain.proceed(message)
    }

}
