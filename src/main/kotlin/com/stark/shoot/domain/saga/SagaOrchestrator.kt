package com.stark.shoot.domain.saga

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Saga 패턴 오케스트레이터
 *
 * 여러 단계를 순차적으로 실행하고, 실패 시 보상 트랜잭션을 역순으로 실행합니다.
 *
 * @param T Saga 컨텍스트 타입
 */
class SagaOrchestrator<T : Any>(
    private val steps: List<SagaStep<T>>
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Saga 실행
     *
     * @param context Saga 컨텍스트
     * @return 성공 여부
     */
    fun execute(context: T): Boolean {
        val executedSteps = mutableListOf<SagaStep<T>>()

        try {
            // 각 단계를 순차적으로 실행
            for (step in steps) {
                logger.info { "Executing saga step: ${step.stepName()}" }

                val success = step.execute(context)

                if (!success) {
                    logger.error { "Saga step failed: ${step.stepName()}" }
                    // 실패 시 보상 트랜잭션 실행
                    compensate(executedSteps, context)
                    return false
                }

                executedSteps.add(step)
                logger.info { "Saga step completed: ${step.stepName()}" }
            }

            logger.info { "Saga completed successfully" }
            return true

        } catch (e: Exception) {
            logger.error(e) { "Saga execution failed with exception" }
            // 예외 발생 시 보상 트랜잭션 실행
            compensate(executedSteps, context)
            return false
        }
    }

    /**
     * 보상 트랜잭션 실행 (역순)
     *
     * @param executedSteps 실행된 단계들
     * @param context Saga 컨텍스트
     */
    private fun compensate(executedSteps: List<SagaStep<T>>, context: T) {
        logger.warn { "Starting compensation for ${executedSteps.size} executed steps" }

        var allCompensationsSucceeded = true

        // 역순으로 보상 트랜잭션 실행
        executedSteps.reversed().forEach { step ->
            try {
                logger.info { "Compensating step: ${step.stepName()}" }
                val success = step.compensate(context)

                if (!success) {
                    logger.error { "Compensation failed for step: ${step.stepName()}" }
                    allCompensationsSucceeded = false
                } else {
                    logger.info { "Compensation completed for step: ${step.stepName()}" }
                }
            } catch (e: Exception) {
                logger.error(e) { "Compensation threw exception for step: ${step.stepName()}" }
                allCompensationsSucceeded = false
            }
        }

        // Context 상태 업데이트 (MessageSagaContext인 경우)
        if (context is com.stark.shoot.domain.saga.message.MessageSagaContext) {
            if (allCompensationsSucceeded) {
                context.markCompensated()
            } else {
                context.markFailed(Exception("Compensation failed for one or more steps"))
            }
        }

        logger.warn { "Compensation process completed: allSucceeded=$allCompensationsSucceeded" }
    }
}
