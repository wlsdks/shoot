package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.shared.UserId

/**
 * 특정 소스의 모든 알림을 읽음 상태로 표시하는 커맨드
 *
 * @property userId 사용자 ID
 * @property sourceType 알림 소스 타입
 * @property sourceId 알림 소스 ID (null이면 해당 소스 타입의 모든 알림)
 */
data class MarkAllNotificationsBySourceAsReadCommand(
    val userId: UserId,
    val sourceType: SourceType,
    val sourceId: String? = null
)