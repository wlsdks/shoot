-- ChatRoomEntity에 version 컬럼 추가
-- Optimistic Locking: 동시 업데이트 시 충돌 감지 및 재시도
--
-- P1-2 개선사항: Netflix 패턴 적용
-- - JPA @Version 어노테이션 사용
-- - 동시에 여러 메시지가 같은 채팅방 메타데이터를 업데이트하는 경우
-- - OptimisticLockException 발생 → 자동 재시도 (최대 3회)
-- - Lost Update 문제 해결

-- 1. 컬럼 추가 (기본값 0)
ALTER TABLE chat_rooms
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 주석 추가
COMMENT ON COLUMN chat_rooms.version IS 'Optimistic Locking 버전 필드 - 동시 업데이트 충돌 감지';
