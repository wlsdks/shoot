package com.stark.shoot.domain.chat.message.service

import com.stark.shoot.domain.chat.message.ChatMessage
import com.stark.shoot.domain.chat.message.type.MessageType
import com.stark.shoot.domain.chatroom.service.EditabilityResult
import com.stark.shoot.infrastructure.exception.MessageException
import java.time.Duration
import java.time.Instant

/**
 * 메시지 편집 관련 도메인 서비스
 * 메시지 편집 가능 여부 확인 및 편집 처리를 담당합니다.
 */
class MessageEditDomainService {

    companion object {
        // 메시지 편집 가능 시간 제한 (24시간)
        private val MAX_EDIT_DURATION = Duration.ofHours(24)
    }

    /**
     * 메시지가 편집 가능한지 확인합니다.
     *
     * @param message 확인할 메시지
     * @return 편집 가능 여부와 사유
     */
    fun canEditMessage(message: ChatMessage): EditabilityResult {
        // 삭제된 메시지 확인
        if (message.content.isDeleted) {
            return EditabilityResult(false, "삭제된 메시지는 수정할 수 없습니다.")
        }

        // 메시지 타입 확인 (TEXT 타입만 수정 가능)
        if (message.content.type != MessageType.TEXT) {
            return EditabilityResult(false, "텍스트 타입의 메시지만 수정할 수 있습니다.")
        }

        // 시간 제한 검사: 생성 후 일정 시간(24시간)이 지난 메시지는 수정 불가
        val now = Instant.now()
        val messageCreationTime = message.createdAt ?: now
        val timeSinceCreation = Duration.between(messageCreationTime, now)

        if (timeSinceCreation.compareTo(MAX_EDIT_DURATION) > 0) {
            return EditabilityResult(
                false,
                "메시지 생성 후 ${MAX_EDIT_DURATION.toHours()}시간이 지나 수정할 수 없습니다."
            )
        }

        return EditabilityResult(true, null)
    }

    /**
     * 메시지를 편집합니다.
     *
     * @param message 편집할 메시지
     * @param newContent 새로운 내용
     * @return 편집된 메시지
     * @throws IllegalArgumentException 메시지가 편집 불가능한 경우
     */
    fun editMessage(
        message: ChatMessage,
        newContent: String
    ): ChatMessage {
        // 편집 가능 여부 확인
        val editabilityResult = canEditMessage(message)
        if (!editabilityResult.canEdit) {
            throw MessageException.NotEditable(editabilityResult.reason)
        }

        // 내용 유효성 검사
        if (newContent.isBlank()) {
            throw MessageException.EmptyContent()
        }

        // 도메인 객체의 메서드를 사용하여 메시지 수정
        message.editMessage(newContent)
        return message
    }

}