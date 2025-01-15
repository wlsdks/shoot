package com.stark.shoot.adapter.out.persistence.mongodb.adapter

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.ChatRoomDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.ChatRoomMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.ChatRoomMongoRepository
import com.stark.shoot.application.port.out.LoadChatRoomPort
import com.stark.shoot.application.port.out.SaveChatRoomPort
import com.stark.shoot.domain.chat.room.ChatRoom
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ChatRoomPersistenceAdapter(
    private val chatRoomRepository: ChatRoomMongoRepository,
    private val chatRoomMapper: ChatRoomMapper,
    private val mongoTemplate: MongoTemplate
) : LoadChatRoomPort, SaveChatRoomPort {

    override fun findById(id: ObjectId): ChatRoom? {
        return chatRoomRepository.findById(id)
            .map(chatRoomMapper::toDomain)
            .orElse(null)
    }

    override fun findByParticipantId(participantId: ObjectId): List<ChatRoom> {
        return chatRoomRepository.findByParticipantsContaining(participantId)
            .map(chatRoomMapper::toDomain)
    }

    override fun existsByParticipants(participants: Set<ObjectId>): Boolean {
        return chatRoomRepository.findByAllParticipants(participants) != null
    }

    override fun save(chatRoom: ChatRoom): ChatRoom {
        val document = chatRoomMapper.toDocument(chatRoom)
        return chatRoomRepository.save(document)
            .let(chatRoomMapper::toDomain)
    }

    override fun updateLastMessage(roomId: ObjectId, lastMessageId: ObjectId) {
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(roomId)),
            Update()
                .set("lastMessageId", lastMessageId)
                .set("lastActiveAt", Instant.now()),
            ChatRoomDocument::class.java
        )
    }

    override fun updateParticipantMetadata(
        roomId: ObjectId,
        participantId: ObjectId,
        lastReadMessageId: ObjectId
    ) {
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(roomId)),
            Update()
                .set("metadata.participantsMetadata.$participantId.lastReadMessageId", lastReadMessageId)
                .set("metadata.participantsMetadata.$participantId.lastReadAt", Instant.now()),
            ChatRoomDocument::class.java
        )
    }

}