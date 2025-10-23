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

## Development Rules

### DO
- Domain 우선 설계 (엔티티, 이벤트 먼저)
- Port 인터페이스 정의 후 구현
- 단일 표현식 함수 사용
- `in` 연산자 사용 (`.contains()` 대신)
- 불필요한 `this` 제거
- Event-driven으로 도메인 간 통신

### DON'T
- Domain에서 infrastructure 직접 의존 금지
- Adapter에 비즈니스 로직 작성 금지
- Controller에서 직접 repository 호출 금지
- 중복 주석 작성 금지

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
