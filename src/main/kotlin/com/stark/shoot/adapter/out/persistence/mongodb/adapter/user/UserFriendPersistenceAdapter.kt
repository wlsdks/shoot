package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.UpdateUserFriendPort
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component

@Component
class UserFriendPersistenceAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val mongoTemplate: MongoTemplate
) : UpdateUserFriendPort {

    /**
     * 친구 요청 추가
     */
    override fun addOutgoingFriendRequest(
        userId: ObjectId,
        targetUserId: ObjectId
    ) {
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(userId)),
            Update().addToSet("outgoingFriendRequests", targetUserId),
            UserDocument::class.java
        )
    }

    /**
     * 친구 요청 추가
     */
    override fun addIncomingFriendRequest(
        userId: ObjectId,
        fromUserId: ObjectId
    ) {
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(userId)),
            Update().addToSet("incomingFriendRequests", fromUserId),
            UserDocument::class.java
        )
    }

    /**
     * 친구 요청 삭제
     */
    override fun removeOutgoingFriendRequest(
        userId: ObjectId,
        targetUserId: ObjectId
    ) {
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(userId)),
            Update().pull("outgoingFriendRequests", targetUserId),
            UserDocument::class.java
        )
    }

    /**
     * 친구 요청 삭제
     */
    override fun removeIncomingFriendRequest(
        userId: ObjectId,
        fromUserId: ObjectId
    ) {
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(userId)),
            Update().pull("incomingFriendRequests", fromUserId),
            UserDocument::class.java
        )
    }

    /**
     * 친구 관계 추가
     */
    override fun addFriendRelation(
        userId: ObjectId,
        friendId: ObjectId
    ) {
        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(userId)),
            Update().addToSet("friends", friendId),
            UserDocument::class.java
        )
    }

}