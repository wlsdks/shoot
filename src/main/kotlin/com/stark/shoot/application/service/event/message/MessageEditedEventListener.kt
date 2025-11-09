package com.stark.shoot.application.service.event.message

import com.stark.shoot.domain.shared.event.MessageEditedEvent
import com.stark.shoot.domain.shared.event.EventVersion
import com.stark.shoot.domain.shared.event.EventVersionValidator
import com.stark.shoot.infrastructure.annotation.ApplicationEventListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener

/**
 * 메시지 수정 이벤트 리스너
 * - 감사 로그 기록
 * - 향후 수정 이력 추적, 컴플라이언스 등에 활용
 */
@ApplicationEventListener
class MessageEditedEventListener {

    private val logger = KotlinLogging.logger {}

    /**
     * 메시지 수정 이벤트 처리
     * MongoDB 저장 완료 후 실행되어 데이터 일관성을 보장합니다.
     *
     * @param event MessageEditedEvent
     */
    @EventListener
    fun handleMessageEdited(event: MessageEditedEvent) {
        // Event Version 검증
        EventVersionValidator.checkAndLog(event, EventVersion.MESSAGE_EDITED_V1, "MessageEditedEventListener")

        logger.info {
            "Message edited: " +
            "messageId=${event.messageId.value}, " +
            "roomId=${event.roomId.value}, " +
            "userId=${event.userId.value}, " +
            "oldContent='${maskContent(event.oldContent)}', " +
            "newContent='${maskContent(event.newContent)}', " +
            "editedAt=${event.editedAt}"
        }

        // TODO: 향후 필요 시 수정 이력 DB 저장 (audit_logs 테이블)
        // TODO: 향후 필요 시 컴플라이언스 시스템 연동
        // TODO: 향후 필요 시 분석 이벤트 전송 (수정 패턴 분석)
    }

    /**
     * 로그용 컨텐츠 마스킹
     * 개인정보 보호를 위해 긴 내용은 일부만 표시합니다.
     */
    private fun maskContent(content: String): String {
        return if (content.length > 50) {
            "${content.take(47)}..."
        } else {
            content
        }
    }
}
