-- =====================================================
-- CASCADE DELETE 제약 조건 추가
-- =====================================================
--
-- 작성일: 2025-11-02
-- 목적: User 삭제 시 관련 데이터 자동 정리
--
-- **배경:**
-- 현재 User 삭제 시 연관된 데이터가 orphaned 상태로 남음
-- - FriendRequest, Friendship, ChatRoomUser 등
--
-- **해결 방법:**
-- ON DELETE CASCADE 제약 조건 추가
-- User 삭제 시 PostgreSQL이 자동으로 관련 데이터 삭제
--
-- **주의사항:**
-- - MongoDB 데이터는 별도 이벤트 리스너로 처리
-- - ChatRoom은 CASCADE 하지 않음 (다른 참여자가 있을 수 있음)
-- =====================================================

-- 1. friend_requests 테이블
-- 기존 FK 제약 조건 삭제 후 CASCADE 옵션과 함께 재생성

-- sender_id FK 재생성
ALTER TABLE friend_requests
    DROP CONSTRAINT IF EXISTS fk_friend_request_sender;

ALTER TABLE friend_requests
    ADD CONSTRAINT fk_friend_request_sender
    FOREIGN KEY (sender_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_friend_request_sender ON friend_requests IS
    'User 삭제 시 해당 사용자가 보낸 모든 친구 요청 자동 삭제';

-- receiver_id FK 재생성
ALTER TABLE friend_requests
    DROP CONSTRAINT IF EXISTS fk_friend_request_receiver;

ALTER TABLE friend_requests
    ADD CONSTRAINT fk_friend_request_receiver
    FOREIGN KEY (receiver_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_friend_request_receiver ON friend_requests IS
    'User 삭제 시 해당 사용자가 받은 모든 친구 요청 자동 삭제';

-- 2. friendship_map 테이블
-- user_id FK 재생성
ALTER TABLE friendship_map
    DROP CONSTRAINT IF EXISTS fk_friendship_user;

ALTER TABLE friendship_map
    ADD CONSTRAINT fk_friendship_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_friendship_user ON friendship_map IS
    'User 삭제 시 해당 사용자의 모든 친구 관계 자동 삭제';

-- friend_id FK 재생성
ALTER TABLE friendship_map
    DROP CONSTRAINT IF EXISTS fk_friendship_friend;

ALTER TABLE friendship_map
    ADD CONSTRAINT fk_friendship_friend
    FOREIGN KEY (friend_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_friendship_friend ON friendship_map IS
    'User 삭제 시 해당 사용자와의 모든 친구 관계 자동 삭제';

-- 3. blocked_users 테이블
-- blocker_id FK 재생성
ALTER TABLE blocked_users
    DROP CONSTRAINT IF EXISTS fk_blocked_users_blocker;

ALTER TABLE blocked_users
    ADD CONSTRAINT fk_blocked_users_blocker
    FOREIGN KEY (blocker_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_blocked_users_blocker ON blocked_users IS
    'User 삭제 시 해당 사용자가 차단한 모든 기록 자동 삭제';

-- blocked_id FK 재생성
ALTER TABLE blocked_users
    DROP CONSTRAINT IF EXISTS fk_blocked_users_blocked;

ALTER TABLE blocked_users
    ADD CONSTRAINT fk_blocked_users_blocked
    FOREIGN KEY (blocked_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_blocked_users_blocked ON blocked_users IS
    'User 삭제 시 해당 사용자가 차단당한 모든 기록 자동 삭제';

-- 4. refresh_tokens 테이블
ALTER TABLE refresh_tokens
    DROP CONSTRAINT IF EXISTS fk_refresh_token_user;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_token_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_refresh_token_user ON refresh_tokens IS
    'User 삭제 시 해당 사용자의 모든 RefreshToken 자동 삭제';

-- 5. friend_groups 테이블
ALTER TABLE friend_groups
    DROP CONSTRAINT IF EXISTS fk_friend_group_user;

ALTER TABLE friend_groups
    ADD CONSTRAINT fk_friend_group_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_friend_group_user ON friend_groups IS
    'User 삭제 시 해당 사용자의 모든 친구 그룹 자동 삭제';

-- 6. friend_group_members 테이블
ALTER TABLE friend_group_members
    DROP CONSTRAINT IF EXISTS fk_friend_group_member_group;

ALTER TABLE friend_group_members
    ADD CONSTRAINT fk_friend_group_member_group
    FOREIGN KEY (friend_group_id)
    REFERENCES friend_groups(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_friend_group_member_group ON friend_group_members IS
    '친구 그룹 삭제 시 해당 그룹의 모든 멤버 자동 삭제';

ALTER TABLE friend_group_members
    DROP CONSTRAINT IF EXISTS fk_friend_group_member_friend;

ALTER TABLE friend_group_members
    ADD CONSTRAINT fk_friend_group_member_friend
    FOREIGN KEY (friend_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_friend_group_member_friend ON friend_group_members IS
    'User 삭제 시 모든 그룹에서 해당 사용자 멤버십 자동 삭제';

-- 7. notifications 테이블
ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS fk_notification_user;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notification_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_notification_user ON notifications IS
    'User 삭제 시 해당 사용자의 모든 알림 자동 삭제';

-- 8. chat_room_users 테이블
-- ⚠️ 주의: ChatRoom은 CASCADE하지 않음
-- User가 나가도 채팅방은 유지되어야 함 (다른 참여자가 있을 수 있음)
ALTER TABLE chat_room_users
    DROP CONSTRAINT IF EXISTS fk_chat_room_user_user;

ALTER TABLE chat_room_users
    ADD CONSTRAINT fk_chat_room_user_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_chat_room_user_user ON chat_room_users IS
    'User 삭제 시 해당 사용자의 모든 채팅방 참여 기록 자동 삭제';

-- ChatRoom 자체는 CASCADE하지 않음 (다른 참여자 보호)
ALTER TABLE chat_room_users
    DROP CONSTRAINT IF EXISTS fk_chat_room_user_room;

ALTER TABLE chat_room_users
    ADD CONSTRAINT fk_chat_room_user_room
    FOREIGN KEY (chat_room_id)
    REFERENCES chat_rooms(id)
    ON DELETE CASCADE;

COMMENT ON CONSTRAINT fk_chat_room_user_room ON chat_room_users IS
    'ChatRoom 삭제 시 해당 채팅방의 모든 참여자 기록 자동 삭제';

-- =====================================================
-- 검증 쿼리 (참고용)
-- =====================================================

-- CASCADE가 설정된 모든 FK 확인
-- SELECT
--     tc.table_name,
--     tc.constraint_name,
--     kcu.column_name,
--     ccu.table_name AS foreign_table_name,
--     ccu.column_name AS foreign_column_name,
--     rc.delete_rule
-- FROM information_schema.table_constraints AS tc
-- JOIN information_schema.key_column_usage AS kcu
--   ON tc.constraint_name = kcu.constraint_name
--   AND tc.table_schema = kcu.table_schema
-- JOIN information_schema.constraint_column_usage AS ccu
--   ON ccu.constraint_name = tc.constraint_name
--   AND ccu.table_schema = tc.table_schema
-- JOIN information_schema.referential_constraints AS rc
--   ON rc.constraint_name = tc.constraint_name
--   AND rc.constraint_schema = tc.table_schema
-- WHERE tc.constraint_type = 'FOREIGN KEY'
--   AND tc.table_schema = 'public'
--   AND rc.delete_rule = 'CASCADE'
-- ORDER BY tc.table_name, tc.constraint_name;
