package com.stark.shoot.adapter.out.persistence.mongodb.document.common

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.Instant

abstract class BaseMongoDocument {
    @Id                          // MongoDB의 _id 필드와 매핑됨
    var id: ObjectId? = null     // null은 새로운 문서 생성 시를 위함
    val createdAt: Instant = Instant.now()
    var updatedAt: Instant? = null
}