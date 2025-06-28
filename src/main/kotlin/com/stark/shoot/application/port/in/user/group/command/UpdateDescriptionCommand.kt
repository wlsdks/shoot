package com.stark.shoot.application.port.`in`.user.group.command

/**
 * Command for updating the description of a friend group
 */
data class UpdateDescriptionCommand(
    val groupId: Long,
    val description: String?
) {
    companion object {
        fun of(groupId: Long, description: String?): UpdateDescriptionCommand {
            return UpdateDescriptionCommand(groupId, description)
        }
    }
}