# Shoot - Real-time Chat Application

> Spring Boot Kotlin 실시간 채팅 애플리케이션

## Tech Stack

- Spring Boot 3.x, Kotlin, Gradle
- WebSocket (STOMP), Redis Stream, Kafka
- PostgreSQL (Users, Friends, Notifications)
- MongoDB (Chat messages, Reactions)
- Redis (Session, Cache, Stream)
- JWT Authentication

## Architecture

- **Hexagonal Architecture** (Ports & Adapters)
- **Domain-Driven Design** (DDD)
- **Event-Driven Architecture**
- **CQRS** (Chat operations)

## Project Structure

```
src/main/kotlin/com/shoot/
├── domain/              # 핵심 비즈니스 로직, 엔티티
├── application/         # Use cases, 서비스 레이어
│   ├── port/in/        # Inbound ports (use cases)
│   ├── port/out/       # Outbound ports (persistence, messaging)
│   └── service/        # 애플리케이션 서비스, 이벤트 리스너
├── adapter/
│   ├── in/             # Controllers, WebSocket handlers
│   └── out/            # DB adapters, messaging adapters
└── infrastructure/      # Config, 공통 기능
```

## Port Naming Convention

- **LoadPort**: 조회 (findById, findAll)
- **SavePort**: 저장/수정 (save, update)
- **QueryPort**: 복잡한 쿼리, 검색
- **CommandPort**: 명령 (create, delete)

## Code Placement

### domain/
- 엔티티, Value Objects
- 도메인 이벤트
- 비즈니스 규칙/로직

### application/
- Use cases (port/in)
- Port interfaces (port/out)
- 애플리케이션 서비스
- 이벤트 리스너 (`@TransactionalEventListener`)

### adapter/
- **in/**: REST Controllers, WebSocket handlers, DTO
- **out/**: JPA/MongoDB repositories, Kafka producers, Redis clients

### infrastructure/
- Configuration (Security, WebSocket, DB)
- 공통 유틸리티, 예외 처리

## Business Rules

### 메시지 (Message)
- 상태: SENDING → SENT_TO_KAFKA → PROCESSING → SAVED / FAILED
- 최대 길이: 4,000자 (DomainConstants)
- 첨부파일: 최대 50MB
- 채팅방당 최대 고정 메시지: 5개
- 삭제: 소프트 삭제 (isDeleted 플래그)
- 수정: TEXT 타입만 가능, 삭제된 메시지 수정 불가
- **수정 시간 제한: 24시간** (생성 후 24시간 이후 수정 불가)
- 빈 내용으로 수정 불가

### 사용자 (User)
- Username: 3-20자
- Nickname: 1-30자
- Password: 최소 8자
- UserCode: 8자리 대문자+숫자, 중복 불가
- 최대 친구 수: 1,000명

### 채팅방 (ChatRoom)
- 1:1 채팅: 정확히 2명
- 그룹 채팅: 2~100명
- 자기 자신과 채팅 생성 불가
- 참여자 없으면 자동 삭제
- 최대 핀 채팅방: 사용자별 제한 (DomainConstants)

### 친구 (Friend)
- 요청 상태: PENDING → ACCEPTED / REJECTED / CANCELLED
- PENDING 상태에서만 처리 가능 (이미 처리된 요청 재처리 불가)
- 추천: BFS 알고리즘 (최대 depth: 3)
- 중복 요청 불가
- **자기 자신에게 친구 요청 불가**
- 이미 친구인 경우 요청 불가
- 이미 보낸 요청이 있으면 재요청 불가
- 상대방으로부터 이미 받은 요청이 있으면 새 요청 불가
- 친구 관계: 양방향 (Friendship 2개 생성)

### 리액션 (Reaction)
- 타입: LIKE, LOVE, HAHA, WOW, SAD, ANGRY
- 사용자당 메시지별 1개 리액션 (다른 리액션 선택 시 교체)

### 채팅방 설정 (ChatRoomSettings)
- 알림 활성화 (isNotificationEnabled, 기본: true)
- 메시지 보존 기간 (retentionDays, 기본: null = 무기한)
- 암호화 설정 (isEncrypted, 기본: false)
- 커스텀 설정 (customSettings: Map<String, Any>)

### WebSocket 제한
- Heartbeat: 5초 (서버 ↔ 클라이언트)
- 메시지 크기: 최대 64KB
- 버퍼 크기: 256KB
- 전송 시간 제한: 10초
- 첫 메시지 대기: 30초
- SockJS disconnect delay: 2초
- Rate Limiting: 타이핑 인디케이터 1초 제한

### 도메인 상수 (DomainConstants)
- `chatRoom.maxParticipants`: 100
- `chatRoom.minGroupParticipants`: 2
- `chatRoom.maxPinnedMessages`: 5
- `message.maxContentLength`: 4000
- `message.maxAttachmentSize`: 52428800 (50MB)
- `message.batchSize`: 100
- `friend.maxFriendCount`: 1000
- `friend.recommendationLimit`: 20

### 이벤트 발행 규칙
- 트랜잭션 커밋 후: `@TransactionalEventListener` 사용
- SpringEventPublisher로 발행
- 이벤트 타입: MESSAGE_CREATED, FRIEND_ADDED, CHAT_ROOM_CREATED 등
- Kafka 토픽: 채팅방 ID 기반 파티셔닝으로 메시지 순서 보장

### 동시성 제어
- Redis 기반 분산락 사용 (`RedisLockManager`)
- 락 키: `chatroom:{roomId}`, `user:{userId}` 등
- 자동 만료 시간 설정으로 데드락 방지
- Lua 스크립트로 안전한 락 해제 (소유자 검증)
- 지수 백오프 재시도 메커니즘
- 채팅방별 독립적 락으로 병렬성 유지

**📖 상세 도메인 모델**: `DOMAIN.md` 참조

## Development Rules

### DO
- Domain 우선 설계 (엔티티, 이벤트 먼저)
- Port 인터페이스 정의 후 구현
- 단일 표현식 함수 사용
- `in` 연산자 사용 (`.contains()` 대신)
- 불필요한 `this` 제거
- Event-driven으로 도메인 간 통신
- DomainConstants에서 상수값 참조

### DON'T
- Domain에서 infrastructure 직접 의존 금지
- Adapter에 비즈니스 로직 작성 금지
- Controller에서 직접 repository 호출 금지
- 중복 주석 작성 금지
- 매직넘버 하드코딩 금지 (DomainConstants 사용)

## Workflow

1. **새 기능 추가**:
   - domain/에 엔티티/값 객체 생성
   - application/port/에 인터페이스 정의
   - application/service/에 Use case 구현
   - adapter/에 구현체 작성
   - 필요시 이벤트 리스너 추가

2. **WebSocket 기능**:
   - `/topic/chat/{roomId}` 토픽 사용
   - Redis Stream으로 메시지 큐잉
   - 재시도 로직 구현
   - 오프라인 사용자는 Redis fallback

3. **이벤트 기반 개발**:
   - domain/event/에 도메인 이벤트 생성
   - `@TransactionalEventListener` 사용
   - SpringEventPublisher로 발행

## Build & Run

```bash
./gradlew bootRun      # 실행
./gradlew test         # 테스트
./gradlew build        # 빌드
```

## Common Issues

### WebSocket 연결 실패
- Redis 연결 확인
- WebSocket 세션 검증 확인

### 메시지 전송 실패
- Kafka 토픽 확인
- Redis Stream 상태 확인

### 인증 오류
- JWT 토큰 유효성 확인
- Security filter 순서 확인

## Testing Strategy

- **Unit**: Domain 로직
- **Integration**: Adapter 레이어
- **WebSocket**: 실시간 기능
- **Event**: 이벤트 처리 로직

## Security

- 모든 엔드포인트 JWT 인증
- WebSocket 세션 검증
- 입력값 검증 (Commands)
- SQL Injection 방지

## Performance

- Redis 캐싱 (자주 조회되는 데이터)
- Kafka 메시지 영속화
- DB Connection pooling
- 비동기 처리 (비중요 작업)

---

*Last updated: 2025-10-23*
