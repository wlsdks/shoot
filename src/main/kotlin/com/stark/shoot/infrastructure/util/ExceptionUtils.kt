package com.stark.shoot.infrastructure.util

import com.stark.shoot.domain.exception.web.ResourceNotFoundException

/**
 * 리소스 조회 결과를 검증하고 null인 경우 ResourceNotFoundException을 발생시킵니다.
 *
 * 여러 서비스에서 반복되는 `?: throw ResourceNotFoundException(...)` 패턴을 통합하여
 * 코드 중복을 제거하고 일관성을 유지합니다.
 *
 * @receiver 조회된 리소스 (nullable)
 * @param message 예외 메시지
 * @return 비-null 리소스
 * @throws ResourceNotFoundException 리소스가 null인 경우
 *
 * @example
 * ```kotlin
 * val message = messageQueryPort.findById(messageId)
 *     .orThrowNotFound("메시지를 찾을 수 없습니다: messageId=$messageId")
 * ```
 */
fun <T : Any> T?.orThrowNotFound(message: String): T =
    this ?: throw ResourceNotFoundException(message)

/**
 * 리소스 조회 결과를 검증하고 null인 경우 ResourceNotFoundException을 발생시킵니다.
 * 리소스 타입과 ID를 기반으로 자동으로 메시지를 생성합니다.
 *
 * @receiver 조회된 리소스 (nullable)
 * @param resourceType 리소스 타입 (예: "메시지", "사용자", "채팅방")
 * @param id 리소스 ID
 * @return 비-null 리소스
 * @throws ResourceNotFoundException 리소스가 null인 경우
 *
 * @example
 * ```kotlin
 * val user = userQueryPort.findById(userId)
 *     .orThrowNotFound("사용자", userId.value)
 * ```
 */
fun <T : Any> T?.orThrowNotFound(resourceType: String, id: Any): T =
    this ?: throw ResourceNotFoundException("${resourceType}를 찾을 수 없습니다: $id")
