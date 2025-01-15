package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.room.ChatRoomDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface ChatRoomMongoRepository : MongoRepository<ChatRoomDocument, ObjectId> {



}