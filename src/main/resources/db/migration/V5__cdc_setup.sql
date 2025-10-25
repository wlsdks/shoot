-- =====================================================
-- CDC (Change Data Capture) 설정
-- =====================================================
--
-- 작성일: 2025-10-25
-- 설명: Debezium CDC를 위한 PostgreSQL Publication 생성
--
-- **요구사항:**
-- - PostgreSQL wal_level=logical 설정 필요
-- - Replication 권한 필요
--
-- **동작:**
-- - outbox_events 테이블의 변경사항을 WAL로 발행
-- - Debezium이 이를 감지하여 Kafka로 전송
--
-- **통합:**
-- - OutboxEventProcessor와 함께 동작 (백업 메커니즘)
-- - CDC가 정상: 밀리초 단위 실시간 발행
-- - CDC 장애 시: OutboxEventProcessor가 5초마다 폴링
-- =====================================================

-- Outbox 테이블을 위한 Publication 생성
-- 이미 존재하는 경우 오류 방지
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication WHERE pubname = 'outbox_publication'
    ) THEN
        CREATE PUBLICATION outbox_publication FOR TABLE outbox_events;
        RAISE NOTICE 'CDC Publication created: outbox_publication';
    ELSE
        RAISE NOTICE 'CDC Publication already exists: outbox_publication';
    END IF;
END
$$;

COMMENT ON PUBLICATION outbox_publication IS 'Debezium CDC를 위한 Outbox 테이블 Publication';

-- Publication 정보 확인 쿼리 (참고용)
-- SELECT * FROM pg_publication WHERE pubname = 'outbox_publication';
-- SELECT * FROM pg_publication_tables WHERE pubname = 'outbox_publication';
