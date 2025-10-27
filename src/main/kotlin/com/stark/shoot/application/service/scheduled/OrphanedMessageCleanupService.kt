package com.stark.shoot.application.service.scheduled

import com.stark.shoot.application.port.out.chatroom.ChatRoomQueryPort
import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chatroom.vo.ChatRoomId
import io.github.oshai.kotlinlogging.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 고아 메시지 정리 스케줄러
 *
 * Saga 보상 트랜잭션 실패로 인해 MongoDB에만 남아있는 메시지를 정리합니다.
 *
 * **고아 메시지 발생 시나리오**:
 * 1. SaveMessageToMongoStep 성공 (MongoDB 저장)
 * 2. UpdateChatRoomMetadataStep 실패 (PostgreSQL 업데이트 실패)
 * 3. 보상 트랜잭션 시작 (SaveMessageToMongoStep.compensate())
 * 4. MongoDB 삭제 실패 (네트워크 오류, MongoDB 다운 등)
 * 5. 결과: MongoDB에만 메시지 존재, PostgreSQL에는 없음
 *
 * **정리 조건**:
 * - createdAt이 7일 이전인 메시지
 * - roomId가 PostgreSQL ChatRoom 테이블에 존재하지 않음
 *
 * **실행 주기**: 매일 새벽 2시
 */
@Service
class OrphanedMessageCleanupService(
    private val mongoTemplate: MongoTemplate,
    private val chatRoomQueryPort: ChatRoomQueryPort,
    private val messageCommandPort: MessageCommandPort
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val MESSAGE_COLLECTION = "messages"
        private const val CLEANUP_AGE_DAYS = 7L
        private const val BATCH_SIZE = 100
    }

    /**
     * 고아 메시지 정리 작업
     *
     * 매일 새벽 2시에 실행되며, ShedLock으로 분산 환경에서 중복 실행을 방지합니다.
     */
    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
    @SchedulerLock(
        name = "cleanupOrphanedMessages",
        lockAtMostFor = "30m",  // 최대 30분 실행
        lockAtLeastFor = "1m"   // 최소 1분 간격
    )
    fun cleanupOrphanedMessages() {
        try {
            logger.info { "Starting orphaned message cleanup task" }
            val startTime = Instant.now()

            // 1. 정리 대상 메시지 조회 (7일 이전)
            val cutoffDate = Instant.now().minus(CLEANUP_AGE_DAYS, ChronoUnit.DAYS)
            val oldMessages = findOldMessages(cutoffDate)

            if (oldMessages.isEmpty()) {
                logger.info { "No old messages found for cleanup" }
                return
            }

            logger.info { "Found ${oldMessages.size} messages older than $CLEANUP_AGE_DAYS days" }

            // 2. 고아 메시지 필터링 및 삭제
            var orphanedCount = 0
            var deletedCount = 0
            var failedCount = 0

            oldMessages.forEach { message ->
                try {
                    val roomId = message["roomId"] as? Long ?: return@forEach
                    val messageId = message["_id"] as? String ?: return@forEach

                    // PostgreSQL에 채팅방이 존재하는지 확인
                    val chatRoomExists = chatRoomQueryPort.findById(ChatRoomId.from(roomId)) != null

                    if (!chatRoomExists) {
                        // 고아 메시지 발견
                        orphanedCount++
                        logger.warn {
                            "Found orphaned message: messageId=$messageId, roomId=$roomId, " +
                                    "createdAt=${message["createdAt"]}"
                        }

                        // MongoDB에서 삭제
                        val deleted = deleteMessage(messageId)
                        if (deleted) {
                            deletedCount++
                        } else {
                            failedCount++
                            logger.error { "Failed to delete orphaned message: $messageId" }
                        }
                    }
                } catch (e: Exception) {
                    failedCount++
                    logger.error(e) { "Error processing message: ${message["_id"]}" }
                }
            }

            val duration = java.time.Duration.between(startTime, Instant.now())
            logger.info {
                "Orphaned message cleanup completed: " +
                        "scanned=${oldMessages.size}, " +
                        "orphaned=$orphanedCount, " +
                        "deleted=$deletedCount, " +
                        "failed=$failedCount, " +
                        "duration=${duration.toMillis()}ms"
            }

            // 실패가 많으면 경고
            if (failedCount > 10) {
                logger.warn { "High failure count ($failedCount) during cleanup - investigate MongoDB health" }
            }

        } catch (e: Exception) {
            logger.error(e) { "Unexpected error in orphaned message cleanup" }
        }
    }

    /**
     * cutoffDate 이전에 생성된 메시지 조회
     */
    private fun findOldMessages(cutoffDate: Instant): List<Map<String, Any>> {
        val query = Query(
            Criteria.where("createdAt").lt(cutoffDate)
        ).limit(BATCH_SIZE)

        @Suppress("UNCHECKED_CAST")
        return mongoTemplate.find(query, Map::class.java, MESSAGE_COLLECTION) as List<Map<String, Any>>
    }

    /**
     * MongoDB에서 메시지 삭제
     */
    private fun deleteMessage(messageId: String): Boolean {
        return try {
            messageCommandPort.delete(MessageId.from(messageId))
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete message: $messageId" }
            false
        }
    }
}
