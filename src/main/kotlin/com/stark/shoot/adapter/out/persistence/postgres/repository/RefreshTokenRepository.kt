package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, Long> {

    fun findByToken(token: String): RefreshTokenEntity?

    @Query("""
        SELECT rt 
        FROM RefreshTokenEntity rt 
        WHERE rt.user.id = :userId
    """)
    fun findAllByUserId(@Param("userId") userId: Long): List<RefreshTokenEntity>

    @Modifying
    @Query("""
        DELETE 
        FROM RefreshTokenEntity rt 
        WHERE rt.token = :token
    """)
    fun deleteByToken(@Param("token") token: String)

    @Modifying
    @Query("""
        DELETE 
        FROM RefreshTokenEntity rt 
        WHERE rt.user.id = :userId
    """)
    fun deleteAllByUserId(@Param("userId") userId: Long)

    @Query("""
        SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END 
        FROM RefreshTokenEntity rt 
        WHERE rt.token = :token 
            AND rt.expirationDate > :currentTime 
            AND rt.isRevoked = false
    """)
    fun isTokenValid(@Param("token") token: String, @Param("currentTime") currentTime: Instant): Boolean

}