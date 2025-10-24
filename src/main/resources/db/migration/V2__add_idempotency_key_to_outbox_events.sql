-- OutboxEventEntity에 idempotency_key 컬럼 추가
-- 멱등성 보장: 중복 요청으로 인한 동일 이벤트 중복 저장 방지
--
-- P1-1 개선사항: Netflix 패턴 적용
-- - 클라이언트 재시도 시 동일한 sagaId로 요청 → 중복 이벤트 저장 방지
-- - 데이터베이스 레벨에서 unique constraint로 보장

-- 1. 컬럼 추가 (기존 데이터는 sagaId를 idempotency_key로 사용)
ALTER TABLE outbox_events
ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

-- 2. 기존 데이터 마이그레이션 (idempotency_key가 null인 경우 saga_id로 채움)
UPDATE outbox_events
SET idempotency_key = saga_id
WHERE idempotency_key IS NULL;

-- 3. NOT NULL 제약 조건 추가
ALTER TABLE outbox_events
ALTER COLUMN idempotency_key SET NOT NULL;

-- 4. Unique 인덱스 추가 (중복 방지)
CREATE UNIQUE INDEX IF NOT EXISTS idx_outbox_idempotency_key
ON outbox_events(idempotency_key);

-- 주석 추가
COMMENT ON COLUMN outbox_events.idempotency_key IS '멱등성 키 - 중복 이벤트 저장 방지 (일반적으로 sagaId 사용)';
