package com.stark.shoot.adapter.out.persistence.mongodb.document.common

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.Instant

abstract class BaseMongoDocument {
    @Id
    var id: ObjectId? = null
    var createdAt: Instant? = Instant.now()
    var updatedAt: Instant? = null
}