package com.stark.shoot.infrastructure.annotation

/**
 * Domain Service를 나타내는 마커 어노테이션
 *
 * DDD에서 Entity나 Value Object에 자연스럽게 속하지 않는 도메인 로직을 표시합니다.
 * Domain Service는 다음 특성을 가집니다:
 * - 상태를 가지지 않습니다(Stateless)
 * - 순수한 도메인 로직을 캡슐화합니다
 * - 여러 Aggregate에 걸친 비즈니스 규칙을 처리합니다
 * - 인프라스트럭처 관심사를 포함하지 않습니다
 *
 * 참고: Application Service와 구분됩니다.
 * - Domain Service: 순수 도메인 로직, 기술 독립적
 * - Application Service: Use Case 조율, 트랜잭션, 영속성 등
 *
 * @see AggregateRoot
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class DomainService
