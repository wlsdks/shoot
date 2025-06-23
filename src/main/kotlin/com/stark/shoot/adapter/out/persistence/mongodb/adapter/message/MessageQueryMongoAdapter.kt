package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.util.toObjectId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@Adapter
class MessageQueryMongoAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val chatMessageMapper: ChatMessageMapper
) : MessageQueryPort {

    /**
     * ID로 채팅 메시지 조회
     *
     * @param messageId 채팅 메시지 ID
     * @return 채팅 메시지
     */
    override fun findById(
        messageId: MessageId
    ): ChatMessage? {
        return chatMessageRepository.findById(messageId.value.toObjectId())
            .map(chatMessageMapper::toDomain)
            .orElse(null)
    }

    /**
     * 채팅방 ID로 채팅 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 목록
     */
    override fun findByRoomId(
        roomId: ChatRoomId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id") // 최신순 정렬
        )

        return chatMessageRepository.findByRoomId(roomId.value, pageable)
            .map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID와 이전 메시지 ID로 이전 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param beforeMessageId 이전 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 목록
     */
    override fun findByRoomIdAndBeforeId(
        roomId: ChatRoomId,
        beforeMessageId: MessageId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id") // 최신순 정렬
        )

        return chatMessageRepository.findByRoomIdAndIdBefore(
            roomId.value,
            beforeMessageId.value.toObjectId(),
            pageable
        ).map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID와 이후 메시지 ID로 이후 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param afterMessageId 이후 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 목록
     */
    override fun findByRoomIdAndAfterId(
        roomId: ChatRoomId,
        afterMessageId: MessageId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.ASC, "_id") // ID 오름차순 정렬 (이후 메시지)
        )

        return chatMessageRepository.findByRoomIdAndIdAfter(
            roomId.value,
            afterMessageId.value.toObjectId(),
            pageable
        ).map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID와 사용자 ID로 읽지 않은 메시지 조회 (페이지네이션 적용)
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @param limit 한 번에 조회할 최대 메시지 수 (기본값: 100)
     * @return 읽지 않은 메시지 목록
     */
    override fun findUnreadByRoomId(
        roomId: ChatRoomId,
        userId: UserId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "createdAt") // 최신순 정렬
        )
        val notReadMessage = chatMessageRepository.findUnreadMessages(roomId.value, userId.value, pageable)
        return notReadMessage.map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID로 고정된 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param limit 조회 개수
     * @return 고정된 메시지 목록
     */
    override fun findPinnedMessagesByRoomId(
        roomId: ChatRoomId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "createdAt") // 최신순 정렬
        )

        // MongoDB 쿼리: {roomId: roomId, "metadata.isPinned": true}
        return chatMessageRepository.findPinnedMessagesByRoomId(roomId.value, pageable)
            .map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID로 메시지 조회 (Flow)
     *
     * @param roomId 채팅방 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 Flow
     */
    override fun findByRoomIdFlow(
        roomId: ChatRoomId,
        limit: Int
    ): Flow<ChatMessage> = flow {
        val messages = findByRoomId(roomId, limit)
        messages.forEach { emit(it) }
    }


    /**
     * 채팅방 ID와 이전 메시지 ID로 이전 메시지 조회 (Flow)
     *
     * @param roomId 채팅방 ID
     * @param beforeMessageId 이전 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 Flow
     */
    override fun findByRoomIdAndBeforeIdFlow(
        roomId: ChatRoomId,
        beforeMessageId: MessageId,
        limit: Int
    ): Flow<ChatMessage> = flow {
        val messages = findByRoomIdAndBeforeId(roomId, beforeMessageId, limit)
        messages.forEach { emit(it) }
    }


    /**
     * 채팅방 ID와 이후 메시지 ID로 이후 메시지 조회 (Flow)
     *
     * @param roomId 채팅방 ID
     * @param afterMessageId 이후 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 Flow
     */
    override fun findByRoomIdAndAfterIdFlow(
        roomId: ChatRoomId,
        afterMessageId: MessageId,
        limit: Int
    ): Flow<ChatMessage> = flow {
        val messages = findByRoomIdAndAfterId(roomId, afterMessageId, limit)
        messages.forEach { emit(it) }
    }

}
