package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.notification.type.NotificationType
import com.stark.shoot.domain.shared.UserId

/**
 * 사용자의 알림을 타입별로 조회하는 커맨드
 *
 * @property userId 사용자 ID
 * @property type 알림 타입
 * @property limit 조회할 알림 개수 (기본값: 20)
 * @property offset 조회 시작 위치 (기본값: 0)
 */
data class GetNotificationsByTypeCommand(
    val userId: UserId,
    val type: NotificationType,
    val limit: Int = 20,
    val offset: Int = 0
)