package com.stark.shoot.adapter.out.persistence.mongodb.repository

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query

interface UserMongoRepository : MongoRepository<UserDocument, ObjectId> {
    fun findByUsername(username: String): UserDocument?
    fun existsByUsername(username: String): Boolean
    fun findByUserCode(userCode: String): UserDocument?
    @Query("{'\$or': [{'username': ?0}, {'userCode': ?1}]}")
    fun findByUsernameOrUserCode(username: String, userCode: String): List<UserDocument>
}