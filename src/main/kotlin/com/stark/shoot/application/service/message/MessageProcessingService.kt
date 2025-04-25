package com.stark.shoot.application.service.message

import com.stark.shoot.application.filter.message.chain.DefaultMessageProcessingChain
import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.redis.RedisLockManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import java.util.*

@UseCase
class MessageProcessingService(
    private val messageProcessingChain: DefaultMessageProcessingChain,
    private val redisLockManager: RedisLockManager
) : ProcessMessageUseCase {

    private val logger = KotlinLogging.logger {}

    @Transactional
    override fun processMessageCreate(message: ChatMessage): ChatMessage {
        // 분산 락 키 생성 (채팅방별로 락을 걸기 위해 사용)
        val lockKey = "chatroom:${message.roomId}"
        val ownerId = "processor-${UUID.randomUUID()}"

        try {
            // 일반 함수 호출로 변경
            return redisLockManager.withLock(lockKey, ownerId) {
                messageProcessingChain.reset().proceed(message)
            }
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 오류: ${message.id}" }
            throw e
        }
    }

}
