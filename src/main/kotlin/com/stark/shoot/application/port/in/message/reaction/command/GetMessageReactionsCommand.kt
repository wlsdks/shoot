package com.stark.shoot.application.port.`in`.message.reaction.command

import com.stark.shoot.domain.chat.message.vo.MessageId

/**
 * Command for getting message reactions
 */
data class GetMessageReactionsCommand(
    val messageId: MessageId
) {
    companion object {
        /**
         * Factory method to create a GetMessageReactionsCommand
         *
         * @param messageId The message ID
         * @return A new GetMessageReactionsCommand
         */
        fun of(messageId: String): GetMessageReactionsCommand {
            return GetMessageReactionsCommand(
                messageId = MessageId.from(messageId)
            )
        }
    }
}