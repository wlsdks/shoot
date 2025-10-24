package com.stark.shoot.domain.saga

/**
 * Saga 실행 상태
 */
enum class SagaState {
    /**
     * Saga가 시작되어 정상 실행 중
     */
    STARTED,

    /**
     * 보상 트랜잭션 실행 중 (롤백 중)
     */
    COMPENSATING,

    /**
     * Saga가 성공적으로 완료됨
     */
    COMPLETED,

    /**
     * Saga가 실패하고 보상도 완료됨
     */
    COMPENSATED,

    /**
     * Saga가 실패했고 보상도 실패함 (수동 개입 필요)
     */
    FAILED
}
