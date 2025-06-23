package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message.thread

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.application.port.out.message.thread.ThreadQueryPort
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.util.toObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@Adapter
class LoadThreadMongoAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val chatMessageMapper: ChatMessageMapper
) : ThreadQueryPort {

    override fun findByThreadId(
        threadId: MessageId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.ASC, "_id")
        )

        return chatMessageRepository.findByThreadId(threadId.value.toObjectId(), pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findByThreadIdAndBeforeId(
        threadId: MessageId,
        beforeMessageId: MessageId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id")
        )

        return chatMessageRepository.findByThreadIdAndIdBefore(
            threadId.value.toObjectId(),
            beforeMessageId.value.toObjectId(),
            pageable
        ).map(chatMessageMapper::toDomain)
    }

    override fun findThreadRootsByRoomId(
        roomId: ChatRoomId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id")
        )

        return chatMessageRepository.findThreadRootsByRoomId(roomId.value, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findThreadRootsByRoomIdAndBeforeId(
        roomId: ChatRoomId,
        beforeMessageId: MessageId,
        limit: Int
    ): List<ChatMessage> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Direction.DESC, "_id")
        )

        return chatMessageRepository.findThreadRootsByRoomIdAndIdBefore(
            roomId.value,
            beforeMessageId.value.toObjectId(),
            pageable
        ).map(chatMessageMapper::toDomain)
    }

    override fun countByThreadId(threadId: MessageId): Long {
        return chatMessageRepository.countByThreadId(threadId.value.toObjectId())
    }

}
