# Flyway Database Migration

> Shoot 애플리케이션의 PostgreSQL 스키마 버전 관리

## 개요

Flyway를 사용하여 데이터베이스 스키마 변경을 버전 관리합니다.

- **마이그레이션 위치**: `src/main/resources/db/migration/`
- **네이밍 규칙**: `V{버전}__{설명}.sql`
- **실행 시점**: 애플리케이션 시작 시 자동 실행

## 마이그레이션 파일

### V1__initial_schema.sql
**초기 스키마 생성**

생성되는 테이블:
1. `users` - 사용자 정보
2. `chat_rooms` - 채팅방
3. `chat_room_users` - 채팅방 참여자
4. `friend_requests` - 친구 요청
5. `friendship_map` - 친구 관계
6. `blocked_users` - 차단 목록
7. `friend_groups` - 친구 그룹
8. `friend_group_members` - 친구 그룹 멤버
9. `refresh_tokens` - JWT Refresh Token
10. `outbox_events` - Saga 패턴 Outbox
11. `outbox_dead_letter_queue` - DLQ
12. `shedlock` - 분산 스케줄러 락

### V2__cdc_setup.sql
**CDC (Change Data Capture) 설정**

- PostgreSQL Publication 생성
- Debezium이 `outbox_events` 테이블 변경사항 감지

## Flyway 설정

**application.yml:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 스키마 검증만 수행
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    validate-on-migrate: true
```

## 사용법

### 1. 로컬 개발 환경

```bash
# 애플리케이션 실행 시 자동으로 마이그레이션 수행
./gradlew bootRun
```

### 2. 기존 데이터베이스에 적용

기존에 `ddl-auto: update`로 생성된 스키마가 있는 경우:

```sql
-- Flyway baseline 설정 (현재 스키마를 V1로 간주)
-- 이미 application.yml에 baseline-on-migrate: true가 설정되어 있음
```

애플리케이션 첫 실행 시:
- Flyway가 자동으로 baseline 생성
- 이후 마이그레이션만 적용

### 3. 마이그레이션 상태 확인

```sql
-- Flyway 메타데이터 테이블
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### 4. 새 마이그레이션 추가

**파일명 규칙:**
```
V{버전}__{설명}.sql

예시:
V3__add_user_settings.sql
V4__alter_chat_room_add_encryption.sql
```

**작성 예시:**
```sql
-- V3__add_user_settings.sql
ALTER TABLE users ADD COLUMN theme VARCHAR(20) DEFAULT 'light';
ALTER TABLE users ADD COLUMN language VARCHAR(10) DEFAULT 'ko';

CREATE INDEX idx_users_theme ON users(theme);
```

## CDC 설정 확인

### Publication 확인
```sql
SELECT * FROM pg_publication WHERE pubname = 'outbox_publication';

-- 상세 정보
SELECT * FROM pg_publication_tables WHERE pubname = 'outbox_publication';
```

### WAL Level 확인
```sql
SHOW wal_level;  -- 결과: logical
```

## 트러블슈팅

### 1. Baseline 오류

**증상:**
```
FlywayException: Found non-empty schema(s) "public" but no schema history table.
```

**해결:**
- `baseline-on-migrate: true` 설정 확인
- 또는 수동 baseline:
```sql
INSERT INTO flyway_schema_history (
    installed_rank, version, description, type, script, checksum,
    installed_by, installed_on, execution_time, success
) VALUES (
    1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL,
    CURRENT_USER, CURRENT_TIMESTAMP, 0, true
);
```

### 2. 마이그레이션 실패 후 복구

**증상:**
```
FlywayException: Validate failed: Migration checksum mismatch
```

**해결:**
```sql
-- 실패한 마이그레이션 레코드 삭제
DELETE FROM flyway_schema_history WHERE success = false;

-- 또는 특정 버전 삭제
DELETE FROM flyway_schema_history WHERE version = '2';
```

### 3. DDL 변경 후 스키마 불일치

**증상:**
```
hibernate.tool.schema.spi.SchemaManagementException
```

**해결:**
1. `ddl-auto: validate` 주석 처리 (임시)
2. 실제 스키마와 엔티티 비교
3. 새 마이그레이션 스크립트 작성
4. `ddl-auto: validate` 복원

## 모범 사례

### DO
- ✅ 모든 스키마 변경은 마이그레이션 스크립트로 작성
- ✅ 파일명에 명확한 설명 포함
- ✅ 각 마이그레이션은 원자적으로 (롤백 가능하게)
- ✅ 주석으로 변경 이유와 영향 설명
- ✅ 개발/스테이징에서 충분히 테스트

### DON'T
- ❌ 이미 적용된 마이그레이션 파일 수정 금지
- ❌ `ddl-auto: update` 사용 금지 (프로덕션)
- ❌ 수동으로 스키마 변경 금지
- ❌ 마이그레이션 없이 엔티티 수정 금지

## 배포 프로세스

### 1. 개발 환경
```bash
# 새 마이그레이션 작성
vim src/main/resources/db/migration/V3__my_change.sql

# 로컬 DB에 적용
./gradlew bootRun

# 검증
psql -d member -c "SELECT * FROM flyway_schema_history;"
```

### 2. 스테이징 환경
```bash
# 배포 전 마이그레이션 확인
./gradlew flywayInfo

# 배포 (자동 마이그레이션)
./gradlew bootRun
```

### 3. 프로덕션 환경
```bash
# 1. 백업 (필수!)
pg_dump -U postgres member > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Dry-run (선택사항)
./gradlew flywayValidate

# 3. 배포
./gradlew bootRun

# 4. 검증
psql -d member -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

## 참고 자료

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [PostgreSQL Logical Replication](https://www.postgresql.org/docs/current/logical-replication.html)
- [Debezium PostgreSQL Connector](https://debezium.io/documentation/reference/stable/connectors/postgresql.html)

---

**마지막 업데이트**: 2025-10-25
