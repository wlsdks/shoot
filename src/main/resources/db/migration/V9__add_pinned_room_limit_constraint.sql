-- =====================================================
-- 핀 고정 채팅방 개수 제한 제약 조건
-- =====================================================
--
-- 작성일: 2025-11-02
-- 목적: 사용자당 최대 핀 고정 채팅방 개수를 DB 레벨에서 강제
--
-- **배경:**
-- DomainConstants에서 maxPinnedRooms = 5로 정의
-- 하지만 애플리케이션 레벨에서만 검증 → 동시 요청 시 우회 가능
--
-- **문제 상황:**
-- Thread 1: 현재 4개 → 검증 통과 → 5번째 핀 추가
-- Thread 2: 현재 4개 → 검증 통과 → 6번째 핀 추가 (초과!)
--
-- **해결 방법:**
-- PostgreSQL Function + Trigger로 DB 레벨에서 강제
-- =====================================================

-- 1. 핀 고정 개수 확인 함수 생성
CREATE OR REPLACE FUNCTION check_pinned_room_limit()
RETURNS TRIGGER AS $$
DECLARE
    pinned_count INTEGER;
    max_pinned_rooms INTEGER := 5; -- DomainConstants.chatRoom.maxPinnedRooms
BEGIN
    -- is_pinned가 true로 변경되는 경우에만 체크
    IF NEW.is_pinned = TRUE AND (TG_OP = 'INSERT' OR OLD.is_pinned = FALSE) THEN
        -- 해당 사용자의 현재 핀 고정 채팅방 개수 조회
        SELECT COUNT(*)
        INTO pinned_count
        FROM chat_room_users
        WHERE user_id = NEW.user_id
          AND is_pinned = TRUE
          AND id != NEW.id; -- 자기 자신 제외 (UPDATE 시)

        -- 최대 개수 초과 시 에러 발생
        IF pinned_count >= max_pinned_rooms THEN
            RAISE EXCEPTION '핀 고정 채팅방은 최대 %개까지만 가능합니다. (현재: %개)',
                max_pinned_rooms, pinned_count
            USING ERRCODE = '23514'; -- check_violation
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION check_pinned_room_limit() IS
    '사용자당 최대 5개까지만 채팅방을 핀 고정할 수 있도록 제한하는 함수';

-- 2. 트리거 생성
DROP TRIGGER IF EXISTS enforce_pinned_room_limit ON chat_room_users;

CREATE TRIGGER enforce_pinned_room_limit
    BEFORE INSERT OR UPDATE ON chat_room_users
    FOR EACH ROW
    WHEN (NEW.is_pinned = TRUE)
    EXECUTE FUNCTION check_pinned_room_limit();

COMMENT ON TRIGGER enforce_pinned_room_limit ON chat_room_users IS
    '핀 고정 채팅방 개수 제한 강제 트리거 (최대 5개)';

-- =====================================================
-- 테스트 쿼리 (참고용)
-- =====================================================

-- 특정 사용자의 핀 고정 채팅방 개수 확인
-- SELECT user_id, COUNT(*) as pinned_count
-- FROM chat_room_users
-- WHERE user_id = 1  -- 테스트할 user_id
--   AND is_pinned = TRUE
-- GROUP BY user_id;

-- 모든 사용자의 핀 고정 채팅방 개수 확인
-- SELECT user_id, COUNT(*) as pinned_count
-- FROM chat_room_users
-- WHERE is_pinned = TRUE
-- GROUP BY user_id
-- ORDER BY pinned_count DESC;

-- 제한을 초과한 사용자 찾기 (마이그레이션 전 데이터 확인)
-- SELECT user_id, COUNT(*) as pinned_count
-- FROM chat_room_users
-- WHERE is_pinned = TRUE
-- GROUP BY user_id
-- HAVING COUNT(*) > 5
-- ORDER BY pinned_count DESC;
