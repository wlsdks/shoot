package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.domain.chat.message.ChatMessage

/**
 * Command for processing a new message
 */
data class ProcessMessageCommand(
    val message: ChatMessage
) {
    companion object {
        /**
         * Factory method to create a ProcessMessageCommand
         *
         * @param message The message to process
         * @return A new ProcessMessageCommand
         */
        fun of(message: ChatMessage): ProcessMessageCommand {
            return ProcessMessageCommand(
                message = message
            )
        }
    }
}