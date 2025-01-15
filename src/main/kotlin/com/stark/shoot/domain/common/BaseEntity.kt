package com.stark.shoot.domain.common

import org.bson.types.ObjectId
import java.time.Instant

abstract class BaseEntity {
    var id: ObjectId? = null
    val createdAt: Instant = Instant.now()
    var updatedAt: Instant? = null
}