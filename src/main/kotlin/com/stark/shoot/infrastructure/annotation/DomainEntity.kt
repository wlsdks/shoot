package com.stark.shoot.infrastructure.annotation

/**
 * Domain Entity를 나타내는 마커 어노테이션
 *
 * DDD에서 식별자로 구분되는 도메인 객체를 표시합니다.
 * Domain Entity는 다음 특성을 가집니다:
 * - 고유한 식별자(Identity)를 가집니다
 * - 생명주기 동안 속성이 변경될 수 있습니다(Mutable)
 * - 동등성(Equality)은 식별자로 판단합니다
 * - Aggregate Root 또는 Aggregate 내부 Entity일 수 있습니다
 *
 * 참고: JPA @Entity와는 다른 개념입니다.
 * - JPA @Entity는 영속성 관심사
 * - @DomainEntity는 도메인 모델링 관심사
 *
 * @see AggregateRoot
 * @see ValueObject
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class DomainEntity
