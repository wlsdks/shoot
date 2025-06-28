package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.application.port.`in`.message.reaction.GetMessageReactionUseCase
import com.stark.shoot.application.port.`in`.message.reaction.command.GetMessageReactionsCommand
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.exception.web.ResourceNotFoundException

@UseCase
class GetMessageReactionService(
    private val messageQueryPort: MessageQueryPort
) : GetMessageReactionUseCase {

    /**
     * 메시지의 모든 리액션을 가져옵니다.
     *
     * @param command 메시지 리액션 조회 커맨드
     * @return 리액션 목록
     */
    override fun getReactions(
        command: GetMessageReactionsCommand
    ): Map<String, Set<Long>> {
        val message = messageQueryPort.findById(command.messageId)
            ?: throw ResourceNotFoundException("메시지를 찾을 수 없습니다: messageId=${command.messageId}")

        // messageReactions.reactions 또는 reactions 속성을 통해 접근 가능
        return message.reactions
    }


    /**
     * 지원하는 리액션 타입 목록을 가져옵니다.
     *
     * @return 리액션 타입 목록
     */
    override fun getSupportedReactionTypes(): List<ReactionType> {
        return ReactionType.entries
    }

}
