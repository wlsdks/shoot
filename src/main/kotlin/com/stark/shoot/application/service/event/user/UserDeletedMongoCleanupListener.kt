package com.stark.shoot.application.service.event.user

import com.stark.shoot.application.port.out.message.MessageCommandPort
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.shared.event.UserDeletedEvent
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async

/**
 * 사용자 삭제 시 MongoDB 데이터 클린업 리스너
 *
 * PostgreSQL ↔ MongoDB 데이터 일관성 유지
 *
 * 문제 상황:
 * - User가 PostgreSQL에서 삭제됨
 * - 하지만 MongoDB에는 해당 사용자가 보낸 메시지가 남아있음
 * - Orphaned documents 발생 (고아 문서)
 *
 * 해결 방법:
 * - UserDeletedEvent 수신 시 MongoDB 메시지 클린업
 * - 비동기 처리로 User 삭제 성능에 영향 없음
 * - 실패해도 로그만 남기고 User 삭제는 성공 (보상 가능)
 */
@ApplicationEventListener
class UserDeletedMongoCleanupListener(
    private val messageQueryPort: MessageQueryPort,
    private val messageCommandPort: MessageCommandPort
) {

    private val logger = KotlinLogging.logger {}

    /**
     * 사용자 삭제 시 MongoDB 메시지 클린업
     *
     * 처리 내용:
     * 1. 해당 사용자가 보낸 모든 메시지 조회
     * 2. 메시지 소프트 삭제 (isDeleted = true)
     * 3. 실패 시 로그만 남김 (User 삭제는 이미 완료됨)
     *
     * @Async: 비동기 처리로 User 삭제 성능에 영향 없음
     */
    @Async
    @EventListener
    fun handleUserDeleted(event: UserDeletedEvent) {
        logger.info {
            "사용자 삭제 감지, MongoDB 메시지 클린업 시작: " +
            "userId=${event.userId.value}, username=${event.username}"
        }

        try {
            // 1. 해당 사용자가 보낸 모든 메시지 조회
            val userMessages = messageQueryPort.findBySenderId(event.userId)

            if (userMessages.isEmpty()) {
                logger.info { "클린업할 메시지 없음: userId=${event.userId.value}" }
                return
            }

            logger.info {
                "총 ${userMessages.size}개의 메시지를 클린업합니다: userId=${event.userId.value}"
            }

            // 2. 모든 메시지 소프트 삭제
            var successCount = 0
            var failCount = 0

            userMessages.forEach { message ->
                try {
                    message.markAsDeleted()
                    messageCommandPort.save(message)
                    successCount++

                    if (successCount % 100 == 0) {
                        logger.debug { "진행 상황: $successCount/${userMessages.size} 메시지 삭제됨" }
                    }
                } catch (e: Exception) {
                    failCount++
                    logger.error(e) {
                        "메시지 삭제 실패: messageId=${message.id?.value}, userId=${event.userId.value}"
                    }
                }
            }

            logger.info {
                "MongoDB 메시지 클린업 완료: userId=${event.userId.value}, " +
                "성공=$successCount, 실패=$failCount, 총=${userMessages.size}"
            }

            // 3. 실패가 많으면 알림 (전체의 10% 이상 실패 시)
            if (failCount > userMessages.size * 0.1) {
                logger.warn {
                    "⚠️ MongoDB 클린업 실패율 높음: userId=${event.userId.value}, " +
                    "실패율=${failCount * 100 / userMessages.size}%"
                }
                // TODO: 관리자 알림 또는 재시도 큐에 추가
            }

        } catch (e: Exception) {
            // 클린업 실패해도 User 삭제는 이미 완료된 상태
            // 로그만 남기고 예외를 전파하지 않음
            logger.error(e) {
                "MongoDB 클린업 실패: userId=${event.userId.value}, " +
                "error=${e.message}"
            }

            // TODO: Dead Letter Queue에 추가하여 나중에 재시도
            // TODO: 관리자 대시보드에 실패 알림
        }
    }
}
