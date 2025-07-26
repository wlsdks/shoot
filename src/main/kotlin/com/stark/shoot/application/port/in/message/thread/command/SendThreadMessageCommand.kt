package com.stark.shoot.application.port.`in`.message.thread.command

import com.stark.shoot.adapter.`in`.rest.dto.message.ChatMessageRequest

/**
 * Command for sending a thread message
 */
data class SendThreadMessageCommand(
    val message: ChatMessageRequest
) {
    companion object {
        /**
         * Factory method to create a SendThreadMessageCommand
         *
         * @param message The message request to send as a thread message
         * @return A new SendThreadMessageCommand
         */
        fun of(message: ChatMessageRequest): SendThreadMessageCommand {
            return SendThreadMessageCommand(
                message = message
            )
        }
    }
}