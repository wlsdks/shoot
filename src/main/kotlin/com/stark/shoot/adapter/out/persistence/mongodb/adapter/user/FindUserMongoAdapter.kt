package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.mongodb.repository.UserMongoRepository
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria

@Adapter
class FindUserMongoAdapter(
    private val userMongoRepository: UserMongoRepository,
    private val userMapper: UserMapper,
    private val mongoTemplate: MongoTemplate
) : FindUserPort {

    /**
     * 사용자명으로 사용자 조회
     *
     * @param username 사용자명
     * @return 사용자
     */
    override fun findByUsername(
        username: String
    ): User? {
        val userDocument = userMongoRepository.findByUsername(username)
        return userDocument?.let { userMapper.toDomain(it) }
    }

    /**
     * ID로 사용자 조회
     *
     * @param id 사용자 ID
     * @return 사용자
     */
    override fun findUserById(
        id: ObjectId
    ): User? {
        val userDocument = userMongoRepository.findById(id)
        return if (userDocument.isPresent) {
            userMapper.toDomain(userDocument.get())
        } else null
    }

    /**
     * 모든 사용자 조회
     *
     * @return 사용자 목록
     */
    override fun findAll(): List<User> {
        val docs = userMongoRepository.findAll()
        return docs.map(userMapper::toDomain)
    }

    /**
     * 사용자 코드로 사용자 조회
     *
     * @param userCode 사용자 코드
     * @return 사용자
     */
    override fun findByUserCode(
        userCode: String
    ): User? {
        val doc = userMongoRepository.findByUserCode(userCode) ?: return null
        return userMapper.toDomain(doc)
    }

    /**
     * 랜덤 사용자 조회
     *
     * @param excludeUserId 제외할 사용자 ID
     * @param limit 조회할 사용자 수
     * @return 사용자 목록
     */
    override fun findRandomUsers(
        excludeUserId: ObjectId,
        limit: Int
    ): List<User> {
        // 1) match _id != excludeUserId
        val matchStage = Aggregation.match(Criteria.where("_id").ne(excludeUserId))

        // 2) sample limit
        val sampleStage = Aggregation.sample(limit.toLong())

        // 3) pipeline
        val pipeline = Aggregation.newAggregation(matchStage, sampleStage)

        val results = mongoTemplate.aggregate(pipeline, "users", UserDocument::class.java)
        val docs = results.mappedResults
        return docs.map(userMapper::toDomain)
    }

    /**
     * 코드로 사용자 조회
     *
     * @param newCode 사용자 코드
     * @return 사용자
     */
    override fun findByCode(
        newCode: String
    ): User? {
        val doc = userMongoRepository.findByUserCode(newCode) ?: return null
        return userMapper.toDomain(doc)
    }

    /**
     * 사용자명 또는 사용자 코드로 사용자 조회
     *
     * @param query 사용자명 또는 사용자 코드
     * @return 사용자 목록
     */
    override fun findByUsernameOrUserCode(
        query: String
    ): List<User> {
        val users = userMongoRepository.findByUsernameOrUserCode(query, query)
        return users.map { userMapper.toDomain(it) }
    }

}