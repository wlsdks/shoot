package com.stark.shoot.infrastructure.config.scheduler

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * ShedLock 설정
 *
 * 분산 환경에서 스케줄러가 중복 실행되지 않도록 보장합니다.
 *
 * **동작 원리**:
 * 1. 스케줄러 실행 전 PostgreSQL에서 락을 획득
 * 2. 락을 획득한 인스턴스만 작업 실행
 * 3. 작업 완료 후 또는 lockAtMostFor 시간 경과 후 락 해제
 *
 * **Netflix/Uber 패턴**:
 * - 여러 서버 인스턴스에서 동일한 Outbox 이벤트를 처리하지 않도록 방지
 * - Database-level locking으로 분산 환경에서 안전성 보장
 *
 * **DB 스키마 요구사항**:
 * shedlock 테이블이 PostgreSQL에 존재해야 합니다:
 * ```sql
 * CREATE TABLE shedlock (
 *     name VARCHAR(64) NOT NULL PRIMARY KEY,
 *     lock_until TIMESTAMP NOT NULL,
 *     locked_at TIMESTAMP NOT NULL,
 *     locked_by VARCHAR(255) NOT NULL
 * );
 * ```
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class ShedLockConfig {

    /**
     * JDBC 기반 LockProvider
     *
     * PostgreSQL을 사용하여 분산 락을 관리합니다.
     */
    @Bean
    fun lockProvider(dataSource: DataSource): LockProvider {
        return JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .usingDbTime() // DB 시간 사용 (서버 시간 불일치 방지)
                .build()
        )
    }
}
