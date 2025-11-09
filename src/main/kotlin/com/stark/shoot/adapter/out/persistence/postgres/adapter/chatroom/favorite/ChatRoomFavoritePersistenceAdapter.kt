package com.stark.shoot.adapter.out.persistence.postgres.adapter.chatroom.favorite

import com.stark.shoot.adapter.out.persistence.postgres.mapper.ChatRoomFavoriteMapper
import com.stark.shoot.adapter.out.persistence.postgres.repository.ChatRoomFavoriteRepository
import com.stark.shoot.application.port.out.chatroom.favorite.ChatRoomFavoriteCommandPort
import com.stark.shoot.application.port.out.chatroom.favorite.ChatRoomFavoriteQueryPort
import com.stark.shoot.domain.chatroom.favorite.ChatRoomFavorite
import com.stark.shoot.domain.chatroom.favorite.vo.ChatRoomFavoriteId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import com.stark.shoot.domain.shared.UserId
import com.stark.shoot.infrastructure.annotation.Adapter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.transaction.annotation.Transactional

/**
 * 채팅방 즐겨찾기 Persistence Adapter
 *
 * ChatRoomFavorite Aggregate의 영속성 관리를 담당합니다.
 * CommandPort와 QueryPort를 모두 구현합니다.
 */
@Adapter
@Transactional
class ChatRoomFavoritePersistenceAdapter(
    private val repository: ChatRoomFavoriteRepository,
    private val mapper: ChatRoomFavoriteMapper
) : ChatRoomFavoriteCommandPort, ChatRoomFavoriteQueryPort {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 100L
    }

    // ========== CommandPort 구현 ==========

    override fun save(chatRoomFavorite: ChatRoomFavorite): ChatRoomFavorite {
        var lastException: Exception? = null

        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                return saveInternal(chatRoomFavorite)
            } catch (e: ObjectOptimisticLockingFailureException) {
                lastException = e
                if (attempt < MAX_RETRY_COUNT - 1) {
                    logger.warn {
                        "Optimistic lock 충돌 발생, 재시도 ${attempt + 1}/$MAX_RETRY_COUNT: " +
                                "userId=${chatRoomFavorite.userId}, chatRoomId=${chatRoomFavorite.chatRoomId}"
                    }
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }

        val exception = lastException
            ?: IllegalStateException("Optimistic lock retry failed but no exception was captured")
        logger.error(exception) {
            "Optimistic lock 재시도 횟수 초과: userId=${chatRoomFavorite.userId}, chatRoomId=${chatRoomFavorite.chatRoomId}"
        }
        throw exception
    }

    private fun saveInternal(chatRoomFavorite: ChatRoomFavorite): ChatRoomFavorite {
        val savedEntity = if (chatRoomFavorite.id != null) {
            // 업데이트
            val existingEntity = repository.findById(chatRoomFavorite.id.value).orElseThrow {
                IllegalStateException("즐겨찾기를 찾을 수 없습니다. id=${chatRoomFavorite.id}")
            }

            existingEntity.update(
                isPinned = chatRoomFavorite.isPinned,
                pinnedAt = chatRoomFavorite.pinnedAt,
                displayOrder = chatRoomFavorite.displayOrder
            )

            repository.save(existingEntity)
        } else {
            // 새로 생성
            val entity = mapper.toEntity(chatRoomFavorite)
            repository.save(entity)
        }

        return mapper.toDomain(savedEntity)
    }

    override fun delete(id: ChatRoomFavoriteId) {
        repository.deleteById(id.value)
    }

    override fun deleteByUserIdAndChatRoomId(userId: UserId, chatRoomId: ChatRoomId) {
        repository.deleteByUserIdAndChatRoomId(userId.value, chatRoomId.value)
    }

    override fun deleteAllByUserId(userId: UserId) {
        repository.deleteAllByUserId(userId.value)
    }

    override fun deleteAllByChatRoomId(chatRoomId: ChatRoomId) {
        repository.deleteAllByChatRoomId(chatRoomId.value)
    }

    // ========== QueryPort 구현 ==========

    @Transactional(readOnly = true)
    override fun findById(id: ChatRoomFavoriteId): ChatRoomFavorite? {
        return repository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByUserIdAndChatRoomId(userId: UserId, chatRoomId: ChatRoomId): ChatRoomFavorite? {
        return repository.findByUserIdAndChatRoomId(userId.value, chatRoomId.value)
            ?.let { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByUserId(userId: UserId, pinnedOnly: Boolean): List<ChatRoomFavorite> {
        val entities = if (pinnedOnly) {
            repository.findAllPinnedByUserId(userId.value)
        } else {
            repository.findAllByUserId(userId.value)
        }

        return entities.map { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun countPinnedByUserId(userId: UserId): Long {
        return repository.countByUserIdAndIsPinned(userId.value, true)
    }

    @Transactional(readOnly = true)
    override fun existsByUserIdAndChatRoomId(userId: UserId, chatRoomId: ChatRoomId): Boolean {
        return repository.existsByUserIdAndChatRoomId(userId.value, chatRoomId.value)
    }

    @Transactional(readOnly = true)
    override fun existsByChatRoomId(chatRoomId: ChatRoomId): Boolean {
        return repository.existsByChatRoomId(chatRoomId.value)
    }
}
