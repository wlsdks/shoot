package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.ChatRoomDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface ChatRoomMongoRepository : MongoRepository<ChatRoomDocument, ObjectId> {

    fun findByParticipantsContaining(participantId: ObjectId): List<ChatRoomDocument>

    @Query("{'participants': {\$all: ?0}}")
    fun findByAllParticipants(participants: Set<ObjectId>): ChatRoomDocument?

    @Query(value = "{'_id': ?0}", fields = "{'metadata.participantsMetadata': 1}")
    fun findParticipantsMetadataById(roomId: ObjectId): ChatRoomDocument?

}