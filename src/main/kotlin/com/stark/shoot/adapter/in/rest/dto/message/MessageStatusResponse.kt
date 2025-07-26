package com.stark.shoot.adapter.`in`.rest.dto.message

import com.stark.shoot.infrastructure.annotation.ApplicationDto

/**
 * 메시지 상태 업데이트를 위한 응답 DTO
 * 클라이언트는 이 객체를 받아 UI에 메시지 상태(전송 중, 저장됨, 실패 등)를 표시합니다.
 */
@ApplicationDto
data class MessageStatusResponse(
    val tempId: String,                // 임시 메시지 ID (클라이언트 추적용)
    val status: String,                // 상태: "sending", "saved", "failed" 등
    val persistedId: String?,          // 영구 저장된 메시지 ID (성공 시)
    val errorMessage: String? = null,  // 오류 메시지 (실패 시)
    val createdAt: String?,            // 생성 시각
)