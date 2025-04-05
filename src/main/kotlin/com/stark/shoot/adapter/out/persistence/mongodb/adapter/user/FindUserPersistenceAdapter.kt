package com.stark.shoot.adapter.out.persistence.mongodb.adapter.user

import com.stark.shoot.adapter.out.persistence.mongodb.mapper.UserMapper
import com.stark.shoot.adapter.out.persistence.postgres.entity.UserEntity
import com.stark.shoot.adapter.out.persistence.postgres.repository.UserRepository
import com.stark.shoot.application.port.out.user.FindUserPort
import com.stark.shoot.domain.chat.user.User
import com.stark.shoot.infrastructure.annotation.Adapter
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

@Adapter
class FindUserPersistenceAdapter(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) : FindUserPort {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * 사용자명으로 사용자 조회
     *
     * @param username 사용자명
     * @return 사용자
     */
    override fun findByUsername(
        username: String
    ): User? {
        val userEntity = userRepository.findByUsername(username)
        return userEntity?.let { userMapper.toDomain(it) }
    }

    /**
     * ID로 사용자 조회
     *
     * @param id 사용자 ID
     * @return 사용자
     */
    override fun findUserById(
        userId: Long
    ): User? {
        val userEntity = userRepository.findById(userId)
        return if (userEntity.isPresent) {
            userMapper.toDomain(userEntity.get())
        } else null
    }

    /**
     * 모든 사용자 조회
     *
     * @return 사용자 목록
     */
    override fun findAll(): List<User> {
        val userDocs = userRepository.findAll()
        return userDocs.map(userMapper::toDomain)
    }

    /**
     * 사용자 코드로 사용자 조회
     *
     * @param userCode 사용자 코드
     * @return 사용자
     */
    override fun findByUserCode(
        userCode: String
    ): User? {
        val userDoc = userRepository.findByUserCode(userCode) ?: return null
        return userMapper.toDomain(userDoc)
    }

    /**
     * 랜덤 사용자 조회 (JPQL 기반)
     *
     * @param excludeUserId 제외할 사용자 ID (Long 타입)
     * @param limit 조회할 사용자 수
     * @return 도메인 객체 User 목록
     */
    override fun findRandomUsers(excludeUserId: Long, limit: Int): List<User> {
        val jpql = "SELECT u FROM UserEntity u WHERE u.id <> :excludeUserId ORDER BY function('RANDOM')"
        val query = entityManager.createQuery(jpql, UserEntity::class.java)
        query.setParameter("excludeUserId", excludeUserId)
        query.maxResults = limit
        val userEntities: List<UserEntity> = query.resultList
        return userEntities.map { userMapper.toDomain(it) }
    }

}