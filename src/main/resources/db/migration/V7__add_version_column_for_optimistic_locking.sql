-- =====================================================
-- 낙관적 락(Optimistic Locking)을 위한 version 컬럼 추가
-- =====================================================
--
-- 작성일: 2025-11-02
-- 목적: Race Condition 방지 (동시성 제어)
--
-- **배경:**
-- BaseEntity를 상속받는 모든 엔티티에 version 필드 추가
-- JPA @Version 어노테이션으로 낙관적 락 구현
--
-- **해결하는 문제:**
-- 1. 친구 요청 동시 취소/수락 → Lost Update
-- 2. RefreshToken 동시 갱신 → 토큰 불일치
-- 3. 기타 동시 수정 충돌
--
-- **동작 방식:**
-- - 엔티티 조회 시: version 값 읽기
-- - 엔티티 수정 시: WHERE id = ? AND version = ?
-- - version 불일치 시: OptimisticLockException 발생
-- - 성공 시: version 자동 증가
--
-- **영향받는 테이블:**
-- BaseEntity를 상속하는 모든 테이블
-- =====================================================

-- 1. users 테이블
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN users.version IS '낙관적 락 버전 (동시성 제어용)';

-- 2. friend_requests 테이블
ALTER TABLE friend_requests
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN friend_requests.version IS '낙관적 락 버전 (친구 요청 동시 처리 방지)';

-- 3. friendship_map 테이블
ALTER TABLE friendship_map
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN friendship_map.version IS '낙관적 락 버전';

-- 4. blocked_users 테이블
ALTER TABLE blocked_users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN blocked_users.version IS '낙관적 락 버전';

-- 5. chat_rooms 테이블 (이미 version 있을 수 있음 - V3에서 추가)
ALTER TABLE chat_rooms
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN chat_rooms.version IS '낙관적 락 버전 (채팅방 정보 동시 수정 방지)';

-- 6. chat_room_users 테이블
ALTER TABLE chat_room_users
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN chat_room_users.version IS '낙관적 락 버전';

-- 7. refresh_tokens 테이블
ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN refresh_tokens.version IS '낙관적 락 버전 (토큰 갱신 동시 처리 방지)';

-- 8. friend_groups 테이블
ALTER TABLE friend_groups
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN friend_groups.version IS '낙관적 락 버전';

-- 9. friend_group_members 테이블
ALTER TABLE friend_group_members
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN friend_group_members.version IS '낙관적 락 버전';

-- 10. notifications 테이블
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN notifications.version IS '낙관적 락 버전';

-- 11. outbox_events 테이블
ALTER TABLE outbox_events
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN outbox_events.version IS '낙관적 락 버전 (이벤트 발행 상태 동시 변경 방지)';

-- 12. outbox_dead_letter_events 테이블
ALTER TABLE outbox_dead_letter_events
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN outbox_dead_letter_events.version IS '낙관적 락 버전';

-- =====================================================
-- 검증 쿼리 (참고용)
-- =====================================================

-- 모든 테이블에 version 컬럼이 추가되었는지 확인
-- SELECT
--     table_name,
--     column_name,
--     data_type,
--     column_default
-- FROM information_schema.columns
-- WHERE column_name = 'version'
--   AND table_schema = 'public'
-- ORDER BY table_name;

-- version 컬럼이 있는 테이블 목록
-- SELECT table_name
-- FROM information_schema.columns
-- WHERE column_name = 'version'
--   AND table_schema = 'public';
