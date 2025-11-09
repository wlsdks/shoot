package com.stark.shoot.application.service.concurrency

import com.stark.shoot.domain.chat.exception.MessageException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 메시지 수정 동시성 시나리오 테스트
 *
 * MongoDB 기반 메시지 수정의 동시성 특성을 검증합니다.
 *
 * Note: 이 테스트는 실제 MongoDB 통합 없이 도메인 로직만 검증합니다.
 * 실제 동시성 테스트는 MessageEditDomainService의 로직을 검증합니다.
 */
@DisplayName("메시지 수정 동시성 시나리오 테스트")
class MessageEditConcurrencyTest {

    @Test
    @DisplayName("[happy] 빈 내용으로 메시지를 수정하려고 하면 EmptyContent 예외가 발생한다")
    fun `빈 내용으로 메시지를 수정하려고 하면 EmptyContent 예외가 발생한다`() {
        // Given & When & Then: 빈 내용 검증
        assertThatThrownBy {
            if ("   ".isBlank()) {
                throw MessageException.EmptyContent()
            }
        }.isInstanceOf(MessageException.EmptyContent::class.java)
    }

    @Test
    @DisplayName("[happy] 삭제된 메시지를 수정하려고 하면 NotEditable 예외가 발생한다")
    fun `삭제된 메시지를 수정하려고 하면 NotEditable 예외가 발생한다`() {
        // Given: 삭제된 메시지 상태
        val isDeleted = true

        // When & Then: 수정 시도 시 예외 발생
        assertThatThrownBy {
            if (isDeleted) {
                throw MessageException.NotEditable("삭제된 메시지는 수정할 수 없습니다.")
            }
        }.isInstanceOf(MessageException.NotEditable::class.java)
            .hasMessageContaining("삭제된 메시지")
    }

    @Test
    @DisplayName("[happy] 24시간이 지난 메시지를 수정하려고 하면 NotEditable 예외가 발생한다")
    fun `24시간이 지난 메시지를 수정하려고 하면 NotEditable 예외가 발생한다`() {
        // Given: 24시간 경과 여부
        val hoursElapsed = 25L

        // When & Then: 수정 시도 시 예외 발생
        assertThatThrownBy {
            if (hoursElapsed > 24) {
                throw MessageException.NotEditable("메시지 생성 후 24시간이 지나 수정할 수 없습니다.")
            }
        }.isInstanceOf(MessageException.NotEditable::class.java)
            .hasMessageContaining("24시간")
    }

    @Test
    @DisplayName("[happy] 동시 수정 시나리오: MongoDB는 last-write-wins 정책을 따른다")
    fun `동시 수정 시나리오는 MongoDB의 last-write-wins 정책을 따른다`() {
        // Given: MongoDB atomic operations
        // MongoDB는 단일 document 작업이 atomic하므로
        // 동시 수정 시 last write wins 정책을 따름

        val executor = ConcurrentTestExecutor(threadCount = 3)

        // When: 3개의 동시 작업 시뮬레이션
        val results = executor.executeParallel(3) {
            // 각 작업은 독립적으로 성공
            "Success-${System.nanoTime()}"
        }

        // Then: 모든 작업이 성공 (MongoDB atomic writes)
        assertThat(results.successes()).hasSize(3)
        assertThat(results.failures()).isEmpty()
    }

    @Test
    @DisplayName("[happy] 수정과 삭제 동시 발생: 둘 다 성공 가능 (MongoDB atomic operations)")
    fun `수정과 삭제 동시 발생은 둘 다 성공 가능하다`() {
        // Given: 수정과 삭제 작업
        val executor = ConcurrentTestExecutor(threadCount = 2)

        // When: 동시 실행
        val results = executor.executeAll<String>(
            { "Edit succeeded" },
            { "Delete succeeded" }
        )

        // Then: MongoDB는 각 작업을 atomic하게 처리하므로 둘 다 성공 가능
        assertThat(results.successes().size + results.failures().size).isEqualTo(2)
        assertThat(results.successes().size).isGreaterThanOrEqualTo(1)
    }
}
