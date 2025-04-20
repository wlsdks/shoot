package com.stark.shoot.application.service.message.schedule

import com.stark.shoot.application.port.out.message.ScheduledMessagePort
import com.stark.shoot.domain.chat.message.ScheduledMessage
import com.stark.shoot.infrastructure.enumerate.ScheduledMessageStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ScheduledMessageProcessor(
    private val scheduledMessagePort: ScheduledMessagePort,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 10초마다 실행되는 스케줄러
     * 예약 시간이 되었거나 지난 메시지를 처리합니다.
     */
    @Scheduled(fixedRate = 10000) // 10초마다 실행
    fun processScheduledMessages() {
        logger.debug { "예약 메시지 처리 작업 실행" }

        val now = Instant.now()
        val messagesToProcess = scheduledMessagePort.findPendingMessagesBeforeTime(now)

        if (messagesToProcess.isEmpty()) {
            logger.debug { "처리할 예약 메시지가 없습니다" }
            return
        }

        logger.info { "처리할 예약 메시지: ${messagesToProcess.size}개" }

        for (message in messagesToProcess) {
            processMessage(message)
        }
    }

    /**
     * 개별 예약 메시지 처리
     */
    private fun processMessage(message: ScheduledMessage) {
        try {
            // 메시지 전송
            // todo: 메시지 전송 필요 (저장 후 웹소켓으로 전달?)

            // 상태 업데이트
            val updatedMessage = message.copy(status = ScheduledMessageStatus.SENT)
            scheduledMessagePort.saveScheduledMessage(updatedMessage)

            logger.info { "예약 메시지 전송 성공: ${message.id}" }
        } catch (e: Exception) {
            logger.error(e) { "예약 메시지 전송 실패: ${message.id}" }
            // 실패한 메시지 처리 방안 (재시도 로직 등) 구현 필요
        }
    }

}