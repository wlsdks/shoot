package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserMongoRepository : MongoRepository<UserDocument, ObjectId> {

    fun findByUsername(username: String): UserDocument?
    fun findByEmail(email: String): UserDocument?
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean

}