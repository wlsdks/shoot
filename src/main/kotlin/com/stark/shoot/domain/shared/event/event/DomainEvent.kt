package com.stark.shoot.domain.shared.event

/**
 * Domain Event 기본 인터페이스
 *
 * 모든 도메인 이벤트는 이 인터페이스를 구현해야 합니다.
 * MSA 환경에서 이벤트 스키마 진화를 위해 version 필드를 포함합니다.
 */
interface DomainEvent {
    /**
     * 이벤트 스키마 버전
     * Semantic Versioning을 따름 (예: 1.0.0)
     */
    val version: EventVersion

    /**
     * 이벤트가 발생한 시각(밀리초)
     */
    val occurredOn: Long
}