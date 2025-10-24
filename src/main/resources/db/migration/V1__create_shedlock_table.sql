-- ShedLock 테이블 생성
-- 분산 스케줄러 동시성 제어를 위한 락 관리 테이블
--
-- 사용처: OutboxEventProcessor의 스케줄러 메서드들
-- - processOutboxEvents (매 5초)
-- - cleanupOldEvents (매일 자정)
-- - monitorFailedEvents (매 시간)

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

-- 인덱스 추가 (선택적, 성능 향상)
CREATE INDEX IF NOT EXISTS idx_shedlock_lock_until ON shedlock(lock_until);

COMMENT ON TABLE shedlock IS '분산 스케줄러 락 관리 테이블 (ShedLock)';
COMMENT ON COLUMN shedlock.name IS '스케줄러 작업 이름 (고유 식별자)';
COMMENT ON COLUMN shedlock.lock_until IS '락 만료 시간 (이 시간 이후 자동 해제)';
COMMENT ON COLUMN shedlock.locked_at IS '락 획득 시간';
COMMENT ON COLUMN shedlock.locked_by IS '락을 획득한 인스턴스 식별자 (호스트명)';
