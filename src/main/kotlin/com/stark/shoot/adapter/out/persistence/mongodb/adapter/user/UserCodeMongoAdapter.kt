package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.application.port.out.user.code.UpdateUserCodePort
import com.stark.shoot.infrastructure.annotation.Adapter
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@Adapter
class UserCodeMongoAdapter(
    private val mongoTemplate: MongoTemplate
) : UpdateUserCodePort {

    /**
     * 사용자 코드 설정
     *
     * @param userId 사용자 ID
     * @param newCode 새로운 사용자 코드
     * @return Unit (void)
     */
    override fun setUserCode(
        userId: ObjectId,
        newCode: String
    ) {
        val query = Query(Criteria.where("_id").`is`(userId))
        val update = Update().set("userCode", newCode)
        mongoTemplate.updateFirst(query, update, UserDocument::class.java)
    }

    /**
     * 사용자 코드 삭제
     *
     * @param userId 사용자 ID
     * @return Unit (void)
     */
    override fun clearUserCode(
        userId: ObjectId
    ) {
        val query = Query(Criteria.where("_id").`is`(userId))
        val update = Update().unset("userCode")
        mongoTemplate.updateFirst(query, update, UserDocument::class.java)
    }

}