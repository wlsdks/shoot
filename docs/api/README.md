# Shoot API Documentation

MSA (Microservice Architecture) 마이그레이션을 위한 API 계약 정의 문서입니다.

## 빠른 시작

### Swagger UI로 API 탐색 (추천)

```bash
# 1. 애플리케이션 실행
./gradlew bootRun

# 2. 브라우저에서 Swagger UI 접속
open http://localhost:8080/swagger-ui.html
```

자세한 사용법은 [Swagger UI 가이드](../SWAGGER_UI_GUIDE.md)를 참조하세요.

### OpenAPI 스펙 파일로 확인

각 서비스별 OpenAPI 3.0 YAML 파일:
- [user-service-api.yaml](./user-service-api.yaml)
- [friend-service-api.yaml](./friend-service-api.yaml)
- [chat-service-api.yaml](./chat-service-api.yaml)
- [notification-service-api.yaml](./notification-service-api.yaml)

---

## 개요

Shoot은 실시간 채팅 애플리케이션으로, 다음과 같은 마이크로서비스로 분리될 예정입니다:

- **User Service**: 사용자 인증, 프로필, 계정 관리
- **Friend Service**: 친구 관계, 친구 요청, 친구 추천
- **Chat Service**: 채팅방, 메시지, 리액션, 스케줄 메시지
- **Notification Service**: 실시간 알림, 푸시 알림, 알림 설정

## API 명세서

### 1. User Service API

**파일**: [user-service-api.yaml](./user-service-api.yaml)

사용자 인증 및 프로필 관리를 담당하는 서비스입니다.

**주요 엔드포인트**:
- `POST /api/v1/auth/register` - 회원가입
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/logout` - 로그아웃
- `POST /api/v1/auth/refresh` - 토큰 갱신
- `GET /api/v1/users/me` - 현재 사용자 정보 조회
- `PUT /api/v1/users/me` - 프로필 업데이트
- `GET /api/v1/users/{userId}` - 사용자 조회
- `GET /api/v1/users/search` - 사용자 검색
- `GET /api/v1/users/code/{userCode}` - UserCode로 사용자 조회
- `DELETE /api/v1/users/me` - 계정 삭제

**주요 도메인**:
- User: 사용자 엔티티 (username, nickname, userCode)
- UserProfile: 사용자 프로필 (bio, avatar, status)
- Authentication: JWT 기반 인증

---

### 2. Friend Service API

**파일**: [friend-service-api.yaml](./friend-service-api.yaml)

친구 관계 및 친구 추천을 담당하는 서비스입니다.

**주요 엔드포인트**:

#### 친구 관계 (6개)
- `GET /api/v1/friends` - 친구 목록 조회
- `GET /api/v1/friends/{friendId}` - 친구 상세 조회
- `DELETE /api/v1/friends/{friendId}` - 친구 삭제
- `GET /api/v1/friends/mutual/{userId}` - 공통 친구 조회
- `GET /api/v1/friends/status/{userId}` - 친구 상태 조회
- `GET /api/v1/friends/count` - 친구 수 조회

#### 친구 요청 (7개)
- `POST /api/v1/friends/requests` - 친구 요청 전송
- `GET /api/v1/friends/requests/received` - 받은 요청 목록
- `GET /api/v1/friends/requests/sent` - 보낸 요청 목록
- `PUT /api/v1/friends/requests/{requestId}/accept` - 요청 수락
- `PUT /api/v1/friends/requests/{requestId}/reject` - 요청 거절
- `DELETE /api/v1/friends/requests/{requestId}` - 요청 취소
- `GET /api/v1/friends/requests/{requestId}` - 요청 상세 조회

#### 친구 추천 (2개)
- `GET /api/v1/friends/suggestions` - 친구 추천 목록
- `GET /api/v1/friends/recommendations` - 추천 친구 (BFS 알고리즘)

**주요 도메인**:
- Friend: 친구 관계 (양방향)
- FriendRequest: 친구 요청 (PENDING, ACCEPTED, REJECTED, CANCELLED)
- FriendRecommendation: BFS 기반 친구 추천 (최대 depth 3)

---

### 3. Chat Service API

**파일**: [chat-service-api.yaml](./chat-service-api.yaml)

채팅방 관리, 메시지 전송, 리액션 등을 담당하는 서비스입니다.

**주요 엔드포인트**:

#### 채팅방 관리 (12개)
- `POST /api/v1/chatrooms` - 채팅방 생성
- `GET /api/v1/chatrooms` - 채팅방 목록 조회
- `GET /api/v1/chatrooms/{roomId}` - 채팅방 상세 조회
- `DELETE /api/v1/chatrooms/{roomId}` - 채팅방 삭제
- `POST /api/v1/chatrooms/{roomId}/participants` - 참여자 추가
- `DELETE /api/v1/chatrooms/{roomId}/participants/{userId}` - 참여자 제거
- `PUT /api/v1/chatrooms/{roomId}/title` - 제목 변경
- `GET /api/v1/chatrooms/{roomId}/participants` - 참여자 목록
- `PUT /api/v1/chatrooms/{roomId}/pin` - 채팅방 고정
- `DELETE /api/v1/chatrooms/{roomId}/pin` - 고정 해제
- `GET /api/v1/chatrooms/pinned` - 고정된 채팅방 목록
- `PUT /api/v1/chatrooms/{roomId}/settings` - 설정 업데이트

#### 메시지 관리 (5개)
- `POST /api/v1/chatrooms/{roomId}/messages` - 메시지 전송
- `GET /api/v1/chatrooms/{roomId}/messages` - 메시지 목록 조회
- `GET /api/v1/chatrooms/{roomId}/messages/{messageId}` - 메시지 상세 조회
- `PUT /api/v1/chatrooms/{roomId}/messages/{messageId}` - 메시지 수정
- `DELETE /api/v1/chatrooms/{roomId}/messages/{messageId}` - 메시지 삭제

#### 리액션 관리 (2개)
- `POST /api/v1/chatrooms/{roomId}/messages/{messageId}/reactions` - 리액션 추가
- `DELETE /api/v1/chatrooms/{roomId}/messages/{messageId}/reactions` - 리액션 제거

#### 스케줄 메시지 (5개)
- `POST /api/v1/chatrooms/{roomId}/scheduled-messages` - 스케줄 메시지 생성
- `GET /api/v1/chatrooms/{roomId}/scheduled-messages` - 스케줄 메시지 목록
- `GET /api/v1/chatrooms/{roomId}/scheduled-messages/{scheduledMessageId}` - 상세 조회
- `PUT /api/v1/chatrooms/{roomId}/scheduled-messages/{scheduledMessageId}` - 수정
- `DELETE /api/v1/chatrooms/{roomId}/scheduled-messages/{scheduledMessageId}` - 취소

#### 메시지 고정 (3개)
- `POST /api/v1/chatrooms/{roomId}/messages/{messageId}/pin` - 메시지 고정
- `DELETE /api/v1/chatrooms/{roomId}/messages/{messageId}/pin` - 고정 해제
- `GET /api/v1/chatrooms/{roomId}/pinned-messages` - 고정 메시지 목록

#### 메시지 전달 & 읽음 (3개)
- `POST /api/v1/chatrooms/{roomId}/messages/{messageId}/forward` - 메시지 전달
- `POST /api/v1/chatrooms/{roomId}/messages/read` - 읽음 처리
- `GET /api/v1/chatrooms/{roomId}/messages/unread-count` - 읽지 않은 개수

**주요 도메인**:
- ChatRoom: 채팅방 (1:1, 그룹)
- Message: 메시지 (TEXT, IMAGE, VIDEO, AUDIO, FILE, SYSTEM)
- Reaction: 리액션 (LIKE, LOVE, HAHA, WOW, SAD, ANGRY)
- ScheduledMessage: 예약 메시지
- MessagePin: 메시지 고정 (채팅방당 최대 5개)

---

### 4. Notification Service API

**파일**: [notification-service-api.yaml](./notification-service-api.yaml)

알림 생성, 조회, 설정 관리를 담당하는 서비스입니다.

**주요 엔드포인트**:

#### 알림 관리 (7개)
- `GET /api/v1/notifications` - 알림 목록 조회
- `GET /api/v1/notifications/{notificationId}` - 알림 상세 조회
- `PUT /api/v1/notifications/{notificationId}/read` - 읽음 처리
- `PUT /api/v1/notifications/read-all` - 모두 읽음 처리
- `DELETE /api/v1/notifications/{notificationId}` - 알림 삭제
- `DELETE /api/v1/notifications` - 모든 알림 삭제
- `GET /api/v1/notifications/unread-count` - 읽지 않은 알림 개수

#### 알림 설정 (4개)
- `GET /api/v1/notifications/settings` - 알림 설정 조회
- `PUT /api/v1/notifications/settings` - 알림 설정 업데이트
- `PUT /api/v1/notifications/settings/sound` - 알림음 설정
- `PUT /api/v1/notifications/settings/vibration` - 진동 설정

**주요 도메인**:
- Notification: 알림 엔티티
- NotificationType: 알림 타입 (MESSAGE, FRIEND_REQUEST, MENTION, REACTION 등)
- NotificationSettings: 사용자별 알림 설정
- SourceType: 알림 출처 (MESSAGE, FRIEND_REQUEST, CHATROOM, SYSTEM)

---

## 이벤트 스키마

**파일**: [../events/EVENT_SCHEMA.md](../events/EVENT_SCHEMA.md)

서비스 간 이벤트 기반 통신을 위한 이벤트 스키마 정의입니다.

**주요 이벤트**:
- Message Events: MessageSentEvent, MessageEditedEvent, MessageDeletedEvent, MessageReactionEvent
- ChatRoom Events: ChatRoomCreatedEvent, ChatRoomParticipantChangedEvent
- Friend Events: FriendAddedEvent, FriendRequestSentEvent
- User Events: UserCreatedEvent, UserDeletedEvent
- Notification Events: NotificationEvent

**Kafka Topic 전략**:
- 토픽 네이밍: `{domain}.{entity}.{event-type}` (예: `chat.message.sent`)
- 파티셔닝: roomId(메시지), userId(사용자/친구) 기반
- 버전 관리: Semantic Versioning (MAJOR.MINOR.PATCH)

---

## 공통 사항

### 인증 (Authentication)

모든 API는 JWT Bearer Token 인증을 사용합니다.

**요청 헤더**:
```
Authorization: Bearer <JWT_TOKEN>
```

**예외**:
- `POST /api/v1/auth/register` - 회원가입
- `POST /api/v1/auth/login` - 로그인

### 응답 포맷

#### 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2025-11-09T01:30:00Z"
}
```

#### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error message",
    "details": { ... }
  },
  "timestamp": "2025-11-09T01:30:00Z"
}
```

### HTTP 상태 코드

- `200 OK`: 성공
- `201 Created`: 리소스 생성 성공
- `204 No Content`: 성공 (응답 본문 없음)
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스 없음
- `409 Conflict`: 중복 또는 충돌
- `500 Internal Server Error`: 서버 오류

### 페이지네이션

목록 조회 API는 공통 페이지네이션 파라미터를 사용합니다:

**요청 파라미터**:
- `page`: 페이지 번호 (0부터 시작, 기본값: 0)
- `size`: 페이지 크기 (기본값: 20, 최대: 100)
- `sort`: 정렬 기준 (예: `createdAt,desc`)

**응답 포맷**:
```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 타임스탬프

모든 타임스탬프는 ISO 8601 형식을 사용합니다:
- 형식: `YYYY-MM-DDTHH:mm:ss.sssZ`
- 예시: `2025-11-09T01:30:00.123Z`
- 타임존: UTC

### API 버전 관리

- 현재 버전: `v1`
- URL 경로에 버전 포함: `/api/v1/...`
- 버전 변경 시 하위 호환성 유지

---

## 도메인 규칙

자세한 비즈니스 규칙은 각 서비스별 API 명세서를 참조하세요.

### 주요 제약사항

**메시지**:
- 최대 길이: 4,000자
- 수정 시간 제한: 생성 후 24시간
- 첨부파일 크기: 최대 50MB

**사용자**:
- Username: 3-20자
- Nickname: 1-30자
- Password: 최소 8자
- UserCode: 8자리 대문자+숫자

**채팅방**:
- 1:1 채팅: 정확히 2명
- 그룹 채팅: 2-100명
- 최대 고정 메시지: 5개

**친구**:
- 최대 친구 수: 1,000명
- 추천 최대 depth: 3 (BFS)

---

## WebSocket

실시간 메시지 및 알림은 WebSocket (STOMP)을 사용합니다.

**엔드포인트**: `ws://localhost:8080/ws`

**주요 토픽**:
- `/topic/chat/{roomId}` - 채팅방 메시지
- `/topic/chat/{roomId}/participants` - 참여자 변경
- `/topic/user/{userId}/notifications` - 사용자별 알림
- `/topic/user/{userId}/typing` - 타이핑 인디케이터

**제한사항**:
- Heartbeat: 5초
- 메시지 크기: 최대 64KB
- Rate Limiting: 타이핑 인디케이터 1초

---

## 기술 스택

- **Framework**: Spring Boot 3.x
- **Language**: Kotlin
- **Message Broker**: Kafka (이벤트 스트리밍)
- **WebSocket**: STOMP over WebSocket
- **Database**:
  - PostgreSQL (User, Friend, Notification)
  - MongoDB (Message, Reaction)
  - Redis (Cache, Session, Stream)
- **Authentication**: JWT
- **API Documentation**: OpenAPI 3.0

---

## 서비스 간 통신

### 동기 통신 (Synchronous)
- REST API (서비스 간 직접 호출)
- 사용 예: 채팅방 생성 시 사용자 검증

### 비동기 통신 (Asynchronous)
- Kafka 이벤트 (이벤트 기반 통신)
- 사용 예: 메시지 전송 → 알림 생성

### 통신 패턴
```
Chat Service (메시지 전송)
    ↓ (이벤트 발행)
Kafka: chat.message.sent
    ↓ (이벤트 구독)
Notification Service (알림 생성)
    ↓ (WebSocket)
User (실시간 알림 수신)
```

---

## 마이그레이션 전략

1. **Phase 1**: Strangler Fig Pattern
   - 기존 모노리스 유지
   - 새로운 기능은 마이크로서비스로 구현
   - API Gateway 도입

2. **Phase 2**: 도메인 분리
   - User Service 분리
   - Friend Service 분리
   - Database 분리

3. **Phase 3**: 핵심 서비스 분리
   - Chat Service 분리 (가장 복잡)
   - Notification Service 분리
   - Kafka 이벤트 기반 통신 전환

4. **Phase 4**: 최적화
   - 서비스 간 통신 최적화
   - 캐싱 전략 개선
   - 모니터링 및 로깅 강화

---

## 관련 문서

- [SWAGGER_UI_GUIDE.md](../SWAGGER_UI_GUIDE.md) - Swagger UI 사용 가이드 ⭐
- [EVENT_SCHEMA.md](../events/EVENT_SCHEMA.md) - 이벤트 스키마
- [CLAUDE.md](../../CLAUDE.md) - 프로젝트 개요 및 아키텍처
- [DOMAIN.md](../../DOMAIN.md) - 도메인 모델 상세

---

**Last updated**: 2025-11-09
**API Version**: v1
**Status**: In Progress (MSA 마이그레이션 준비 중)
