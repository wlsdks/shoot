package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user.friend

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.friend.UpdateFriendPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@Adapter
class FriendMongoAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val mongoTemplate: MongoTemplate,
    private val userMapper: UserMapper
) : UpdateFriendPort {

    /**
     * 친구 요청 추가 (친구 요청 보내기)
     *
     * @param userId 유저의 ObjectId (몽고 DB의 _id)
     * @param targetUserId 친구 요청을 받을 유저의 ObjectId (몽고 DB의 _id)
     * @return Unit (void)
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
     * 친구 요청 추가 (친구 요청 받기)
     *
     * @param userId 유저의 ObjectId (몽고 DB의 _id)
     * @param fromUserId 친구 요청을 보낸 유저의 ObjectId (몽고 DB의 _id)
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
     * 친구 요청 삭제 (친구 요청 취소)
     *
     * @param userId 유저의 ObjectId (몽고 DB의 _id)
     * @param targetUserId 친구 요청을 받을 유저의 ObjectId (몽고 DB의 _id)
     * @return Unit (void)
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
     * 친구 요청 삭제 (친구 요청 거절)
     *
     * @param userId 유저의 ObjectId (몽고 DB의 _id)
     * @param fromUserId 친구 요청을 보낸 유저의 ObjectId (몽고 DB의 _id)
     * @return Unit (void)
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
     *
     * @param userId 유저의 ObjectId (몽고 DB의 _id)
     * @param friendId 친구의 ObjectId (몽고 DB의 _id)
     * @return Unit (void)
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

    /**
     * 친구 관계 업데이트
     *
     * @param user 사용자
     * @return 업데이트된 사용자
     */
    override fun updateFriends(
        user: User
    ): User {
        val userDocument = userMapper.toDocument(user)
        val updatedUser = userMongoRepository.save(userDocument)
        return userMapper.toDomain(updatedUser)
    }

}