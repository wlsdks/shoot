-- =====================================================
-- ChatRoomFavorite Aggregate 테이블 생성
-- =====================================================
--
-- 작성일: 2025-11-09
-- 목적: DDD 개선 - 즐겨찾기를 별도 Aggregate로 분리
--
-- **배경:**
-- 기존: chat_room_users.is_pinned로 관리 → ChatRoom Aggregate의 일부
-- 문제: 사용자의 개인 설정이 ChatRoom 상태에 포함되어 동시성 충돌 발생
--
-- **개선:**
-- - ChatRoomFavorite를 독립적인 Aggregate Root로 분리
-- - 사용자별 즐겨찾기 설정이 독립적으로 관리됨
-- - 동시성 충돌 제거 (여러 사용자가 동시에 즐겨찾기 가능)
-- - Transaction Boundary 명확화
-- =====================================================

-- 1. chat_room_favorites 테이블 생성
CREATE TABLE IF NOT EXISTS chat_room_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chat_room_id BIGINT NOT NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT TRUE,
    pinned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    display_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    -- Unique constraint: 한 사용자는 동일 채팅방에 하나의 즐겨찾기만 가능
    CONSTRAINT uk_chat_room_favorite_user_room UNIQUE (user_id, chat_room_id)
);

-- 2. 인덱스 생성
CREATE INDEX idx_chat_room_favorite_user_id ON chat_room_favorites(user_id);
CREATE INDEX idx_chat_room_favorite_chat_room_id ON chat_room_favorites(chat_room_id);
CREATE INDEX idx_chat_room_favorite_user_pinned ON chat_room_favorites(user_id, is_pinned);

-- 3. 테이블 코멘트
COMMENT ON TABLE chat_room_favorites IS '채팅방 즐겨찾기 Aggregate (DDD)';
COMMENT ON COLUMN chat_room_favorites.user_id IS '사용자 ID';
COMMENT ON COLUMN chat_room_favorites.chat_room_id IS '채팅방 ID';
COMMENT ON COLUMN chat_room_favorites.is_pinned IS '고정 여부';
COMMENT ON COLUMN chat_room_favorites.pinned_at IS '고정 시간';
COMMENT ON COLUMN chat_room_favorites.display_order IS '표시 순서 (null = 자동 정렬)';
COMMENT ON COLUMN chat_room_favorites.version IS '낙관적 락 버전';

-- 4. 기존 데이터 마이그레이션 (chat_room_users.is_pinned → chat_room_favorites)
INSERT INTO chat_room_favorites (user_id, chat_room_id, is_pinned, pinned_at, created_at)
SELECT
    cru.user_id,
    cru.chat_room_id,
    TRUE as is_pinned,
    cru.created_at as pinned_at,
    cru.created_at
FROM chat_room_users cru
WHERE cru.is_pinned = TRUE
ON CONFLICT (user_id, chat_room_id) DO NOTHING;

-- 5. 핀 고정 개수 확인 함수 생성 (V9의 old 함수 대체)
DROP TRIGGER IF EXISTS enforce_pinned_room_limit ON chat_room_users;
DROP FUNCTION IF EXISTS check_pinned_room_limit();

CREATE OR REPLACE FUNCTION check_chat_room_favorite_limit()
RETURNS TRIGGER AS $$
DECLARE
    pinned_count INTEGER;
    max_pinned_rooms INTEGER := 20; -- ChatRoomConstants.maxPinnedChatRooms
BEGIN
    -- is_pinned가 true로 변경되는 경우에만 체크
    IF NEW.is_pinned = TRUE AND (TG_OP = 'INSERT' OR OLD.is_pinned = FALSE) THEN
        -- 해당 사용자의 현재 핀 고정 채팅방 개수 조회
        SELECT COUNT(*)
        INTO pinned_count
        FROM chat_room_favorites
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

COMMENT ON FUNCTION check_chat_room_favorite_limit() IS
    '사용자당 최대 20개까지만 채팅방을 핀 고정할 수 있도록 제한하는 함수 (ChatRoomFavorite Aggregate)';

-- 6. 트리거 생성
CREATE TRIGGER enforce_chat_room_favorite_limit
    BEFORE INSERT OR UPDATE ON chat_room_favorites
    FOR EACH ROW
    WHEN (NEW.is_pinned = TRUE)
    EXECUTE FUNCTION check_chat_room_favorite_limit();

COMMENT ON TRIGGER enforce_chat_room_favorite_limit ON chat_room_favorites IS
    '핀 고정 채팅방 개수 제한 강제 트리거 (최대 20개)';

-- 7. chat_room_users.is_pinned 컬럼 제거
--    (기존 코드와의 호환성을 위해 주석 처리, 추후 제거 가능)
-- ALTER TABLE chat_room_users DROP COLUMN IF EXISTS is_pinned;

-- =====================================================
-- 테스트 쿼리 (참고용)
-- =====================================================

-- 특정 사용자의 핀 고정 채팅방 개수 확인
-- SELECT user_id, COUNT(*) as pinned_count
-- FROM chat_room_favorites
-- WHERE user_id = 1  -- 테스트할 user_id
--   AND is_pinned = TRUE
-- GROUP BY user_id;

-- 모든 사용자의 핀 고정 채팅방 개수 확인
-- SELECT user_id, COUNT(*) as pinned_count
-- FROM chat_room_favorites
-- WHERE is_pinned = TRUE
-- GROUP BY user_id
-- ORDER BY pinned_count DESC;

-- 마이그레이션 검증: chat_room_users.is_pinned와 chat_room_favorites 비교
-- SELECT
--     cru.user_id,
--     cru.chat_room_id,
--     cru.is_pinned as old_pinned,
--     crf.is_pinned as new_pinned
-- FROM chat_room_users cru
-- LEFT JOIN chat_room_favorites crf
--     ON cru.user_id = crf.user_id AND cru.chat_room_id = crf.chat_room_id
-- WHERE cru.is_pinned = TRUE;
