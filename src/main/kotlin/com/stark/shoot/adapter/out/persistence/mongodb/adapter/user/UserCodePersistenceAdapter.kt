package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.document.user.UserDocument
import com.stark.shoot.application.port.out.user.code.UpdateUserCodePort
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component

@Component
class UserCodePersistenceAdapter(
    private val mongoTemplate: MongoTemplate
) : UpdateUserCodePort {

    /**
     * 사용자 코드 설정
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
     */
    override fun clearUserCode(
        userId: ObjectId
    ) {
        val query = Query(Criteria.where("_id").`is`(userId))
        val update = Update().unset("userCode")
        mongoTemplate.updateFirst(query, update, UserDocument::class.java)
    }

}