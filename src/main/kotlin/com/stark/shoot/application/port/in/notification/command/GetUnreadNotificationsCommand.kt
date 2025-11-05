package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.shared.UserId

/**
 * 사용자의 읽지 않은 알림 목록을 조회하는 커맨드
 *
 * @property userId 사용자 ID
 * @property limit 조회할 알림 개수 (기본값: 20)
 * @property offset 조회 시작 위치 (기본값: 0)
 */
data class GetUnreadNotificationsCommand(
    val userId: UserId,
    val limit: Int = 20,
    val offset: Int = 0
)