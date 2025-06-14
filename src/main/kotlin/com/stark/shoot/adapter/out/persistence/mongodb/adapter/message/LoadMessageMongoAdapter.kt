package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.Adapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@Adapter
class LoadMessageMongoAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val chatMessageMapper: ChatMessageMapper
) : LoadMessagePort {

    /**
     * ID로 채팅 메시지 조회
     *
     * @param id 채팅 메시지 ID
     * @return 채팅 메시지
     */
    override fun findById(
        id: ObjectId
    ): ChatMessage? {
        return chatMessageRepository.findById(id)
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
        roomId: Long,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id") // 최신순 정렬
        )

        return chatMessageRepository.findByRoomId(roomId, pageable)
            .map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID와 이전 메시지 ID로 이전 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param lastId 이전 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 목록
     */
    override fun findByRoomIdAndBeforeId(
        roomId: Long,
        lastId: ObjectId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id") // 최신순 정렬
        )

        return chatMessageRepository.findByRoomIdAndIdBefore(roomId, lastId, pageable)
            .map(chatMessageMapper::toDomain)
    }

    /**
     * 채팅방 ID와 이후 메시지 ID로 이후 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param lastId 이후 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 목록
     */
    override fun findByRoomIdAndAfterId(
        roomId: Long,
        lastId: ObjectId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.ASC, "_id") // ID 오름차순 정렬 (이후 메시지)
        )

        return chatMessageRepository.findByRoomIdAndIdAfter(roomId, lastId, pageable)
            .map(chatMessageMapper::toDomain)
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
        roomId: Long,
        userId: Long,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "createdAt") // 최신순 정렬
        )
        val notReadMessage = chatMessageRepository.findUnreadMessages(roomId, userId, pageable)
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
        roomId: Long,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "createdAt") // 최신순 정렬
        )

        // MongoDB 쿼리: {roomId: roomId, "metadata.isPinned": true}
        return chatMessageRepository.findPinnedMessagesByRoomId(roomId, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findByThreadId(
        threadId: ObjectId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.ASC, "_id")
        )

        return chatMessageRepository.findByThreadId(threadId, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findByThreadIdAndBeforeId(
        threadId: ObjectId,
        lastId: ObjectId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id")
        )

        return chatMessageRepository.findByThreadIdAndIdBefore(threadId, lastId, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findThreadRootsByRoomId(
        roomId: Long,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id")
        )

        return chatMessageRepository.findThreadRootsByRoomId(roomId, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findThreadRootsByRoomIdAndBeforeId(
        roomId: Long,
        lastId: ObjectId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id")
        )

        return chatMessageRepository.findThreadRootsByRoomIdAndIdBefore(roomId, lastId, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun countByThreadId(threadId: ObjectId): Long {
        return chatMessageRepository.countByThreadId(threadId)
    }

    /**
     * 채팅방 ID로 메시지 조회 (Flow)
     *
     * @param roomId 채팅방 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 Flow
     */
    override fun findByRoomIdFlow(
        roomId: Long,
        limit: Int
    ): Flow<ChatMessage> = flow {
        val messages = findByRoomId(roomId, limit)
        messages.forEach { emit(it) }
    }


    /**
     * 채팅방 ID와 이전 메시지 ID로 이전 메시지 조회 (Flow)
     *
     * @param roomId 채팅방 ID
     * @param messageId 이전 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 Flow
     */
    override fun findByRoomIdAndBeforeIdFlow(
        roomId: Long,
        messageId: ObjectId,
        limit: Int
    ): Flow<ChatMessage> = flow {
        val messages = findByRoomIdAndBeforeId(roomId, messageId, limit)
        messages.forEach { emit(it) }
    }


    /**
     * 채팅방 ID와 이후 메시지 ID로 이후 메시지 조회 (Flow)
     *
     * @param roomId 채팅방 ID
     * @param messageId 이후 메시지 ID
     * @param limit 조회 개수
     * @return 채팅 메시지 Flow
     */
    override fun findByRoomIdAndAfterIdFlow(
        roomId: Long,
        messageId: ObjectId,
        limit: Int
    ): Flow<ChatMessage> = flow {
        val messages = findByRoomIdAndAfterId(roomId, messageId, limit)
        messages.forEach { emit(it) }
    }

}
