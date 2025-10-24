package com.stark.shoot.domain.saga

/**
 * Saga의 개별 단계를 나타내는 인터페이스
 *
 * @param T Saga 컨텍스트 타입
 */
interface SagaStep<T> {
    /**
     * 단계 실행
     * @return 성공 여부
     */
    fun execute(context: T): Boolean

    /**
     * 보상 트랜잭션 (롤백)
     * @return 성공 여부
     */
    fun compensate(context: T): Boolean

    /**
     * 단계 이름 (로깅용)
     */
    fun stepName(): String
}
