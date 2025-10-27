-- PostgreSQL CDC 초기화 스크립트
-- Debezium이 Outbox 테이블의 변경사항을 읽을 수 있도록 Publication 생성

-- 1. Publication 생성 (Outbox 테이블만 포함)
-- Publication은 Debezium이 구독할 수 있는 변경 스트림을 정의합니다
-- PostgreSQL 13에서는 IF NOT EXISTS를 지원하지 않으므로 조건부로 생성
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'outbox_publication') THEN
        CREATE PUBLICATION outbox_publication FOR TABLE outbox_events;
        RAISE NOTICE 'Publication outbox_publication created';
    ELSE
        RAISE NOTICE 'Publication outbox_publication already exists';
    END IF;
END $$;

-- 2. Replication Slot은 Debezium Connector가 자동으로 생성하므로 여기서는 생략

-- 3. 권한 확인용 쿼리 (로그 출력)
DO $$
BEGIN
    RAISE NOTICE 'CDC Publication created successfully for outbox_events table';
    RAISE NOTICE 'Debezium can now subscribe to changes on this table';
END $$;

-- 4. Publication 확인
SELECT
    pubname AS publication_name,
    puballtables AS all_tables,
    pubinsert AS track_insert,
    pubupdate AS track_update,
    pubdelete AS track_delete
FROM pg_publication
WHERE pubname = 'outbox_publication';

-- 5. Publication이 포함하는 테이블 확인
SELECT
    schemaname,
    tablename
FROM pg_publication_tables
WHERE pubname = 'outbox_publication';
