package com.stark.shoot.adapter.out.persistence.mongodb.adapter.message

import com.fasterxml.jackson.databind.ObjectMapper
import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest
import com.stark.shoot.adapter.`in`.web.dto.message.read.ReadStatus
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.ChatMessageDocument
import com.stark.shoot.adapter.out.persistence.mongodb.document.message.bookmark.MessageBookmarkDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatMessageMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatMessageMongoRepository
import com.stark.shoot.adapter.out.persistence.mongodb.repository.MessageBookmarkMongoRepository
import com.stark.shoot.application.port.out.message.MessagePort
import com.stark.shoot.domain.chat.bookmark.MessageBookmark
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.user.vo.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import com.stark.shoot.infrastructure.util.toObjectId
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.bson.types.ObjectId
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate

@Adapter
class MessageMongoAdapter(
    private val chatMessageRepository: ChatMessageMongoRepository,
    private val bookmarkRepository: MessageBookmarkMongoRepository,
    private val chatMessageMapper: ChatMessageMapper,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : MessagePort {

    private val logger = KotlinLogging.logger {}

    override fun save(message: ChatMessage): ChatMessage {
        val document = chatMessageMapper.toDocument(message)
        return chatMessageRepository.save(document).let(chatMessageMapper::toDomain)
    }

    override fun saveAll(messages: List<ChatMessage>): List<ChatMessage> {
        val docs = messages.map(chatMessageMapper::toDocument)
        return chatMessageRepository.saveAll(docs).map(chatMessageMapper::toDomain).toList()
    }

    override fun findById(messageId: MessageId): ChatMessage? {
        return chatMessageRepository.findById(messageId.value.toObjectId())
            .map(chatMessageMapper::toDomain)
            .orElse(null)
    }

    override fun findByRoomId(roomId: ChatRoomId, limit: Int): List<ChatMessage> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id"))
        return chatMessageRepository.findByRoomId(roomId.value, pageable).map(chatMessageMapper::toDomain)
    }

    override fun findByRoomIdAndBeforeId(roomId: ChatRoomId, beforeMessageId: MessageId, limit: Int): List<ChatMessage> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id"))
        return chatMessageRepository.findByRoomIdAndIdBefore(roomId.value, beforeMessageId.value.toObjectId(), pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findByRoomIdAndAfterId(roomId: ChatRoomId, afterMessageId: MessageId, limit: Int): List<ChatMessage> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "_id"))
        return chatMessageRepository.findByRoomIdAndIdAfter(roomId.value, afterMessageId.value.toObjectId(), pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findUnreadByRoomId(roomId: ChatRoomId, userId: UserId, limit: Int): List<ChatMessage> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        return chatMessageRepository.findUnreadMessages(roomId.value, userId.value, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findPinnedMessagesByRoomId(roomId: ChatRoomId, limit: Int): List<ChatMessage> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        return chatMessageRepository.findPinnedMessagesByRoomId(roomId.value, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findByRoomIdFlow(roomId: ChatRoomId, limit: Int): Flow<ChatMessage> = flow {
        val messages = findByRoomId(roomId, limit)
        messages.forEach { emit(it) }
    }

    override fun findByRoomIdAndBeforeIdFlow(roomId: ChatRoomId, beforeMessageId: MessageId, limit: Int): Flow<ChatMessage> = flow {
        val messages = findByRoomIdAndBeforeId(roomId, beforeMessageId, limit)
        messages.forEach { emit(it) }
    }

    override fun findByRoomIdAndAfterIdFlow(roomId: ChatRoomId, afterMessageId: MessageId, limit: Int): Flow<ChatMessage> = flow {
        val messages = findByRoomIdAndAfterId(roomId, afterMessageId, limit)
        messages.forEach { emit(it) }
    }

    override fun findByThreadId(threadId: MessageId, limit: Int): List<ChatMessage> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "_id"))
        return chatMessageRepository.findByThreadId(threadId.value.toObjectId(), pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findByThreadIdAndBeforeId(threadId: MessageId, beforeMessageId: MessageId, limit: Int): List<ChatMessage> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id"))
        return chatMessageRepository.findByThreadIdAndIdBefore(threadId.value.toObjectId(), beforeMessageId.value.toObjectId(), pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findThreadRootsByRoomId(roomId: ChatRoomId, limit: Int): List<ChatMessage> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id"))
        return chatMessageRepository.findThreadRootsByRoomId(roomId.value, pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun findThreadRootsByRoomIdAndBeforeId(roomId: ChatRoomId, beforeMessageId: MessageId, limit: Int): List<ChatMessage> {
        val pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "_id"))
        return chatMessageRepository.findThreadRootsByRoomIdAndIdBefore(roomId.value, beforeMessageId.value.toObjectId(), pageable)
            .map(chatMessageMapper::toDomain)
    }

    override fun countByThreadId(threadId: MessageId): Long {
        return chatMessageRepository.countByThreadId(threadId.value.toObjectId())
    }

    override suspend fun publish(message: ChatMessageRequest) {
        val streamKey = "stream:chat:room:${message.roomId}"
        val messageJson = objectMapper.writeValueAsString(message)
        val map = mapOf("message" to messageJson)
        val record = StreamRecords.newRecord().ofMap(map).withStreamKey(streamKey)
        redisTemplate.opsForStream<String, String>().add(record)
    }

    override fun saveBookmark(bookmark: MessageBookmark): MessageBookmark {
        val document = MessageBookmarkDocument.fromDomain(bookmark)
        val saved = bookmarkRepository.save(document)
        return saved.toDomain()
    }

    override fun deleteBookmark(messageId: MessageId, userId: UserId) {
        bookmarkRepository.deleteByMessageIdAndUserId(messageId.value, userId.value)
    }

    override fun findBookmarksByUser(userId: UserId, roomId: ChatRoomId?): List<MessageBookmark> {
        val documents = bookmarkRepository.findByUserId(userId.value)
        if (roomId == null) {
            return documents.map { it.toDomain() }
        }

        return documents.filter { doc ->
            val message = chatMessageRepository.findById(ObjectId(doc.messageId))
            message.isPresent && message.get().roomId == roomId.value
        }.map { it.toDomain() }
    }

    override fun exists(messageId: MessageId, userId: UserId): Boolean {
        return bookmarkRepository.existsByMessageIdAndUserId(messageId.value, userId.value)
    }

    private fun key(roomId: ChatRoomId, userId: UserId): String = "read:status:${roomId.value}:${userId.value}"

    override fun save(readStatus: ReadStatus): ReadStatus {
        val key = key(readStatus.roomId, readStatus.userId)
        val value = objectMapper.writeValueAsString(readStatus)
        redisTemplate.opsForValue().set(key, value)
        return readStatus
    }

    override fun updateLastReadMessageId(roomId: ChatRoomId, userId: UserId, messageId: MessageId): ReadStatus {
        val current = findByRoomIdAndUserId(roomId, userId) ?: ReadStatus.create(roomId, userId)
        val updated = current.markAsRead(messageId)
        return save(updated)
    }

    override fun incrementUnreadCount(roomId: ChatRoomId, userId: UserId): ReadStatus {
        val current = findByRoomIdAndUserId(roomId, userId) ?: ReadStatus.create(roomId, userId)
        val updated = current.incrementUnreadCount()
        return save(updated)
    }

    override fun resetUnreadCount(roomId: ChatRoomId, userId: UserId): ReadStatus {
        val current = findByRoomIdAndUserId(roomId, userId) ?: ReadStatus.create(roomId, userId)
        val updated = current.copy(unreadCount = 0)
        return save(updated)
    }

    override fun findByRoomIdAndUserId(roomId: ChatRoomId, userId: UserId): ReadStatus? {
        val value = redisTemplate.opsForValue().get(key(roomId, userId)) ?: return null
        return objectMapper.readValue(value, ReadStatus::class.java)
    }

    override fun findAllByRoomId(roomId: ChatRoomId): List<ReadStatus> {
        val pattern = "read:status:${roomId.value}:*"
        return redisTemplate.keys(pattern).mapNotNull { k ->
            redisTemplate.opsForValue().get(k)?.let { objectMapper.readValue(it, ReadStatus::class.java) }
        }
    }
}
