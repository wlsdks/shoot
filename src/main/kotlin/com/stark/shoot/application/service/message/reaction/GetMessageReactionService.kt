package com.stark.shoot.application.service.message.reaction

import com.stark.shoot.application.port.`in`.message.reaction.GetMessageReactionUseCase
import com.stark.shoot.application.port.`in`.message.reaction.command.GetMessageReactionsCommand
import com.stark.shoot.application.port.out.message.MessageQueryPort
import com.stark.shoot.application.port.out.message.reaction.MessageReactionQueryPort
import com.stark.shoot.domain.chat.reaction.type.ReactionType
import com.stark.shoot.infrastructure.annotation.UseCase
import com.stark.shoot.infrastructure.util.orThrowNotFound

@UseCase
class GetMessageReactionService(
    private val messageQueryPort: MessageQueryPort,
    private val messageReactionQueryPort: MessageReactionQueryPort
) : GetMessageReactionUseCase {

    /**
     * 메시지의 모든 리액션을 가져옵니다.
     *
     * 새로운 MessageReaction Aggregate를 사용하여 조회합니다.
     *
     * @param command 메시지 리액션 조회 커맨드
     * @return 리액션 목록 (타입별 사용자 ID 목록)
     */
    override fun getReactions(
        command: GetMessageReactionsCommand
    ): Map<String, Set<Long>> {
        // 메시지 존재 확인
        messageQueryPort.findById(command.messageId)
            .orThrowNotFound("메시지", command.messageId)

        // MessageReaction Aggregate에서 리액션 조회
        val reactions = messageReactionQueryPort.findAllByMessageId(command.messageId)

        // Map<ReactionType, Set<UserId>> 형태로 변환
        return reactions
            .groupBy { it.reactionType.code }
            .mapValues { (_, reactionList) ->
                reactionList.map { it.userId.value }.toSet()
            }
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
