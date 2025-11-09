package com.stark.shoot.infrastructure.annotation

/**
 * Aggregate Root를 나타내는 마커 어노테이션
 *
 * DDD에서 Aggregate의 진입점이 되는 엔티티를 표시합니다.
 * Aggregate Root는 다음 특성을 가집니다:
 * - Aggregate 내부의 모든 객체에 대한 접근을 통제합니다
 * - 트랜잭션 일관성의 경계를 정의합니다
 * - 외부에서는 반드시 Aggregate Root를 통해서만 내부 객체에 접근해야 합니다
 * - 다른 Aggregate와는 ID를 통해서만 참조합니다
 *
 * @see ValueObject
 * @see DomainEntity
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AggregateRoot
