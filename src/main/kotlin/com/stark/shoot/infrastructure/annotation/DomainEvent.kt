package com.stark.shoot.infrastructure.annotation

/**
 * Domain Event를 나타내는 마커 어노테이션
 *
 * DDD에서 도메인에서 발생한 의미 있는 사건을 표시합니다.
 * Domain Event는 다음 특성을 가집니다:
 * - 과거 시제로 명명됩니다 (예: MessageSent, UserRegistered)
 * - 불변(Immutable)입니다
 * - 발생 시점(Timestamp)을 포함합니다
 * - Aggregate 간 느슨한 결합을 위해 사용됩니다
 * - 최종 일관성(Eventual Consistency)을 구현하는 수단입니다
 *
 * @see AggregateRoot
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class DomainEvent
