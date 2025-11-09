package com.stark.shoot.infrastructure.annotation

/**
 * Value Object를 나타내는 마커 어노테이션
 *
 * DDD에서 값으로 식별되는 불변 객체를 표시합니다.
 * Value Object는 다음 특성을 가집니다:
 * - 불변(Immutable)입니다
 * - 동등성(Equality)은 속성 값으로 판단합니다
 * - 식별자(Identity)가 없습니다
 * - 부작용 없는(Side-effect free) 함수만 제공합니다
 *
 * Kotlin에서는 주로 data class 또는 @JvmInline value class로 구현됩니다.
 *
 * @see AggregateRoot
 * @see DomainEntity
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ValueObject
