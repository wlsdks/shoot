package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.message.LoadMessagePort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.Adapter
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
        roomId: ObjectId,
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
        roomId: ObjectId,
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
        roomId: ObjectId,
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
     * 채팅방 ID와 사용자 ID로 읽지 않은 메시지 조회
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 읽지 않은 메시지 목록
     */
    override fun findUnreadByRoomId(
        roomId: ObjectId,
        userId: ObjectId
    ): List<ChatMessage> {
        val notReadMessage = chatMessageRepository.findUnreadMessages(roomId, userId.toString())
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
        roomId: ObjectId,
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

}