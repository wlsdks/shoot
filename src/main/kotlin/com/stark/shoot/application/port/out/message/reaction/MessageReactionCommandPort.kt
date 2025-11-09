package com.stark.shoot.application.port.out.message.reaction

import com.stark.shoot.domain.chat.message.vo.MessageId
import com.stark.shoot.domain.chat.reaction.MessageReaction
import com.stark.shoot.domain.chat.reaction.vo.MessageReactionId
import com.stark.shoot.domain.shared.UserId

/**
 * 메시지 리액션 명령 포트
 *
 * 리액션 생성, 수정, 삭제를 담당합니다.
 */
interface MessageReactionCommandPort {

    /**
     * 리액션 저장
     *
     * @param reaction 저장할 리액션
     * @return 저장된 리액션
     */
    fun save(reaction: MessageReaction): MessageReaction

    /**
     * 리액션 삭제
     *
     * @param id 삭제할 리액션 ID
     */
    fun delete(id: MessageReactionId)

    /**
     * 사용자의 메시지 리액션 삭제
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     */
    fun deleteByMessageIdAndUserId(messageId: MessageId, userId: UserId)

    /**
     * 메시지의 모든 리액션 삭제
     *
     * @param messageId 메시지 ID
     */
    fun deleteAllByMessageId(messageId: MessageId)
}
