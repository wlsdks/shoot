package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.user.vo.UserId

/**
 * 사용자의 읽지 않은 알림 개수를 조회하는 커맨드
 *
 * @property userId 사용자 ID
 */
data class GetUnreadNotificationCountCommand(
    val userId: UserId
)