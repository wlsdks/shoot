package com.stark.shoot.adapter.out.persistence.postgres.repository

import com.stark.shoot.adapter.out.persistence.postgres.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, Long> {

    fun findByToken(token: String): RefreshTokenEntity?

    @Query(
        """
        SELECT r 
        FROM RefreshTokenEntity r 
        WHERE r.user.id = :userId
    """
    )
    fun findAllByUserId(@Param("userId") userId: Long): List<RefreshTokenEntity>

    @Modifying
    @Query(
        """
        UPDATE RefreshTokenEntity r 
        SET r.isRevoked = true 
        WHERE r.user.id = :userId
    """
    )
    fun revokeAllByUserId(@Param("userId") userId: Long): Int

    @Modifying
    @Query(
        """
        DELETE 
        FROM RefreshTokenEntity r 
        WHERE r.expirationDate < :before 
            OR r.isRevoked = true
    """
    )
    fun deleteAllByExpirationDateBeforeOrIsRevokedTrue(@Param("before") before: Instant): Int

}