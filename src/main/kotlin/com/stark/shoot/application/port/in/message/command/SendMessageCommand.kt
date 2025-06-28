package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.adapter.`in`.web.dto.message.ChatMessageRequest

/**
 * Command for sending a message
 */
data class SendMessageCommand(
    val message: ChatMessageRequest
) {
    companion object {
        /**
         * Factory method to create a SendMessageCommand
         *
         * @param message The message request to send
         * @return A new SendMessageCommand
         */
        fun of(message: ChatMessageRequest): SendMessageCommand {
            return SendMessageCommand(
                message = message
            )
        }
    }
}