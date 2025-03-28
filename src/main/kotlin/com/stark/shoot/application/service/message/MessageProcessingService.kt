package com.stark.shoot.application.service.message

import com.stark.shoot.application.port.`in`.message.ProcessMessageUseCase
import com.stark.shoot.application.filter.message.chain.DefaultMessageProcessingChain
import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.config.redis.RedisLockManager
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

@UseCase
class MessageProcessingService(
    private val messageProcessingChain: DefaultMessageProcessingChain,
    private val redisLockManager: RedisLockManager
) : ProcessMessageUseCase {

    private val logger = KotlinLogging.logger {}

    override suspend fun processMessageCreate(message: ChatMessage): ChatMessage {
        // 분산 락 키 생성 (채팅방별로 락을 걸기 위해 사용)
        val lockKey = "chatroom:${message.roomId}"
        val ownerId = "processor-${UUID.randomUUID()}"
        val startTime = System.currentTimeMillis()

        try {
            // 코루틴 블록 내에서 분산 락 획득
            return redisLockManager.withLockSuspend(lockKey, ownerId) {
                // 필터 체인 실행 (체인을 재사용하기 위해 reset 호출)
                messageProcessingChain.reset().proceed(message)
            }
        } catch (e: Exception) {
            logger.error(e) { "메시지 처리 오류: ${message.id}" }
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logger.debug { "메시지 처리 소요 시간: ${duration}ms, messageId: ${message.id}" }
        }
    }

}