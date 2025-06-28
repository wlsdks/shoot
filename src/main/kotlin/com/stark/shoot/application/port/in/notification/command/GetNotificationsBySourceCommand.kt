package com.stark.shoot.application.port.`in`.notification.command

import com.stark.shoot.domain.notification.type.SourceType
import com.stark.shoot.domain.user.vo.UserId

/**
 * 사용자의 알림을 소스별로 조회하는 커맨드
 *
 * @property userId 사용자 ID
 * @property sourceType 알림 소스 타입
 * @property sourceId 알림 소스 ID (null이면 해당 소스 타입의 모든 알림)
 * @property limit 조회할 알림 개수 (기본값: 20)
 * @property offset 조회 시작 위치 (기본값: 0)
 */
data class GetNotificationsBySourceCommand(
    val userId: UserId,
    val sourceType: SourceType,
    val sourceId: String? = null,
    val limit: Int = 20,
    val offset: Int = 0
)