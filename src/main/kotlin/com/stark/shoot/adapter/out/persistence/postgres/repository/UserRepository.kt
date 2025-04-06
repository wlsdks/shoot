package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun findByUsername(username: String): UserEntity?
    fun findByUserCode(userCode: String): UserEntity?
}