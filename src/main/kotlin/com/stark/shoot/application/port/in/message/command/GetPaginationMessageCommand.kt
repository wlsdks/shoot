package com.stark.shoot.application.port.`in`.message.command

import com.stark.shoot.adapter.`in`.socket.dto.SyncRequestDto

/**
 * Command for getting paginated messages
 */
data class GetPaginationMessageCommand(
    val request: SyncRequestDto
) {
    companion object {
        /**
         * Factory method to create a GetPaginationMessageCommand
         *
         * @param request The sync request
         * @return A new GetPaginationMessageCommand
         */
        fun of(request: SyncRequestDto): GetPaginationMessageCommand {
            return GetPaginationMessageCommand(
                request = request
            )
        }
    }
}