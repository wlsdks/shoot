package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByUsername(username: String): UserEntity?
    fun findByUserCode(userCode: String): UserEntity?

    @Modifying
    @Query("UPDATE UserEntity u SET u.userCode = :userCode WHERE u.id = :userId")
    fun updateUserCode(@Param("userId") userId: Long, @Param("userCode") userCode: String)
}
