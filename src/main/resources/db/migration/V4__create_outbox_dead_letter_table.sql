-- Outbox Dead Letter Queue 테이블 생성
-- 재시도 횟수를 초과한 실패 이벤트를 저장하는 테이블
--
-- 목적:
-- - 영구적으로 실패한 이벤트를 별도 관리
-- - 운영자 수동 확인 및 재처리 가능
-- - 실패 원인 분석 및 디버깅
-- - Slack 알림으로 즉시 문제 인지
--
-- Netflix/Uber 패턴:
-- - 모든 프로덕션 시스템은 DLQ 필수
-- - 자동 재시도 + 수동 재처리 조합
-- - 실패 이벤트 유실 방지

CREATE TABLE IF NOT EXISTS outbox_dead_letter (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,

    -- 원본 Outbox 이벤트 참조
    original_event_id BIGINT NOT NULL,

    -- Saga 추적
    saga_id VARCHAR(100) NOT NULL,
    saga_state VARCHAR(20) NOT NULL,

    -- 이벤트 정보
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,

    -- 실패 정보
    failure_reason TEXT NOT NULL,
    failure_count INT NOT NULL,
    last_failure_at TIMESTAMP NOT NULL,

    -- 생성 시간
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 해결 상태
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolution_note TEXT
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_dlq_saga_id ON outbox_dead_letter(saga_id);
CREATE INDEX IF NOT EXISTS idx_dlq_created_at ON outbox_dead_letter(created_at);
CREATE INDEX IF NOT EXISTS idx_dlq_resolved ON outbox_dead_letter(resolved);
CREATE INDEX IF NOT EXISTS idx_dlq_original_event_id ON outbox_dead_letter(original_event_id);

-- 주석 추가
COMMENT ON TABLE outbox_dead_letter IS 'Outbox Dead Letter Queue - 실패한 이벤트 저장';
COMMENT ON COLUMN outbox_dead_letter.id IS 'DLQ 고유 ID';
COMMENT ON COLUMN outbox_dead_letter.original_event_id IS '원본 Outbox 이벤트 ID';
COMMENT ON COLUMN outbox_dead_letter.saga_id IS 'Saga 추적 ID';
COMMENT ON COLUMN outbox_dead_letter.saga_state IS 'Saga 최종 상태 (FAILED, COMPENSATED 등)';
COMMENT ON COLUMN outbox_dead_letter.event_type IS '이벤트 타입 (클래스명)';
COMMENT ON COLUMN outbox_dead_letter.payload IS '이벤트 페이로드 (JSON)';
COMMENT ON COLUMN outbox_dead_letter.failure_reason IS '실패 원인';
COMMENT ON COLUMN outbox_dead_letter.failure_count IS '재시도 횟수';
COMMENT ON COLUMN outbox_dead_letter.last_failure_at IS '마지막 실패 시간';
COMMENT ON COLUMN outbox_dead_letter.created_at IS 'DLQ 생성 시간';
COMMENT ON COLUMN outbox_dead_letter.resolved IS '해결 여부 (운영자 처리 완료)';
COMMENT ON COLUMN outbox_dead_letter.resolved_at IS '해결 시간';
COMMENT ON COLUMN outbox_dead_letter.resolved_by IS '해결자 (관리자 ID)';
COMMENT ON COLUMN outbox_dead_letter.resolution_note IS '해결 방법 메모';
