package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.shared.UserId

/**
 * 모든 알림 삭제 커맨드
 *
 * @property userId 사용자 ID
 */
data class DeleteAllNotificationsCommand(
    val userId: UserId
)