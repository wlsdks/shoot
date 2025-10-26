-- =====================================================
-- ShedLock 분산 락 테이블
-- =====================================================
--
-- 작성일: 2025-10-26
-- 설명: OutboxEventProcessor 중복 실행 방지를 위한 ShedLock 테이블
--
-- **용도:**
-- - 여러 애플리케이션 인스턴스가 동시에 실행될 때
-- - OutboxEventProcessor가 한 인스턴스에서만 실행되도록 보장
-- - 분산 환경에서 스케줄러 중복 실행 방지
--
-- **락 메커니즘:**
-- - processOutboxEvents: OutboxEventProcessor 스케줄러 락
-- - lock_until: 락이 유효한 시간 (일반적으로 10초)
-- - locked_by: 락을 획득한 인스턴스 식별자
-- =====================================================

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

COMMENT ON TABLE shedlock IS 'ShedLock 분산 락 테이블 - 스케줄러 중복 실행 방지';
COMMENT ON COLUMN shedlock.name IS '락 이름 (스케줄러 메서드명)';
COMMENT ON COLUMN shedlock.lock_until IS '락 만료 시간';
COMMENT ON COLUMN shedlock.locked_at IS '락 획득 시간';
COMMENT ON COLUMN shedlock.locked_by IS '락 획득 인스턴스 ID';

-- 샘플 락 레코드 (참고용)
-- INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
-- VALUES ('processOutboxEvents', NOW() + INTERVAL '10 seconds', NOW(), 'instance-001');
