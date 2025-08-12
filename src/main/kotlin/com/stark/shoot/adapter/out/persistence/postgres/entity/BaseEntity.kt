package com.stark.shoot.adapter.out.persistence.postgres.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0

    @CreatedDate
    @Column(nullable = false, updatable = false)
    open val createdAt: Instant = Instant.now()

    @LastModifiedDate
    open var updatedAt: Instant? = null

    override fun equals(other: Any?): Boolean =
        this === other || (other is BaseEntity && id == other.id)

    override fun hashCode(): Int = id.hashCode()

}