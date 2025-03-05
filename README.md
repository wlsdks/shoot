# 웹소켓 기반 실시간 채팅 애플리케이션 "Shoot"

## 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [기술 스택](#기술-스택)
3. [아키텍처](#아키텍처)
4. [핵심 기능 구현](#핵심-기능-구현)
   - [인증 시스템](#인증-시스템)
   - [실시간 채팅](#실시간-채팅)
   - [타이핑 인디케이터](#타이핑-인디케이터)
   - [메시지 읽음 처리](#메시지-읽음-처리)
   - [친구 관리 및 추천 시스템](#친구-관리-및-추천-시스템)
   - [채팅방 관리](#채팅방-관리)
5. [데이터 흐름](#데이터-흐름)
6. [확장성 및 고가용성](#확장성-및-고가용성)
7. [개발 환경 설정](#개발-환경-설정)
8. [향후 개선 사항](#향후-개선-사항)

## 프로젝트 개요

Shoot은 Spring Boot(Kotlin)과 WebSocket 기술을 활용한 실시간 채팅 애플리케이션입니다. 헥사고날 아키텍처를 기반으로 설계되었으며, 메시지 전송의 안정성과 확장성을 위해 Redis와 Kafka를 활용하고 있습니다. 주요 특징으로는 실시간 타이핑 인디케이터, 메시지 읽음 상태 추적, 친구 추천 시스템 등이 있습니다.

## 기술 스택

### 백엔드
- **Spring Boot 3.4.3 (Kotlin 1.9.25)** - 애플리케이션 서버
- **Spring WebSocket** - 실시간 양방향 통신
- **Spring Security** - 인증 및 권한 관리
- **MongoDB** - 주 데이터베이스 (사용자, 채팅방, 메시지 저장)
- **Redis** - 캐싱, 메시지 브로커, 실시간 상태 관리
- **Kafka** - 메시지 큐잉 및 처리
- **JWT** - 토큰 기반 인증

## 아키텍처

Shoot은 헥사고날 아키텍처를 채택하여 도메인 중심의 개발 방식을 따르고 있습니다. 이 아키텍처는 애플리케이션의 핵심 비즈니스 로직을 외부 의존성으로부터 격리하고, 테스트 용이성을 높이며, 확장성을 개선합니다.

### 패키지 구조

```
com.stark.shoot
├── adapter                 # 외부 시스템과의 인터페이스
│   ├── in                  # 인바운드 어댑터 (컨트롤러, 웹소켓 핸들러 등)
│   │   ├── event           # 이벤트 리스너
│   │   ├── kafka           # Kafka 소비자
│   │   ├── redis           # Redis 메시지 리스너
│   │   └── web             # REST API 컨트롤러, WebSocket 핸들러
│   └── out                 # 아웃바운드 어댑터 (레포지토리, 이벤트 발행자 등)
│       ├── event           # 이벤트 발행 어댑터
│       ├── kafka           # Kafka 프로듀서
│       └── persistence     # 데이터베이스 어댑터 (MongoDB)
├── application             # 애플리케이션 로직
│   ├── port                # 포트 정의 (인터페이스)
│   │   ├── in              # 인바운드 포트 (서비스 인터페이스)
│   │   └── out             # 아웃바운드 포트 (저장소, 메시징 인터페이스)
│   └── service             # 비즈니스 로직 구현 (유스케이스)
├── domain                  # 도메인 모델
│   ├── chat                # 채팅 관련 도메인
│   │   ├── event           # 도메인 이벤트
│   │   ├── message         # 메시지 모델
│   │   ├── room            # 채팅방 모델
│   │   └── user            # 사용자 모델
│   └── common              # 공통 도메인 모델
└── infrastructure          # 공통 인프라 설정
    ├── config              # 스프링 설정 (보안, 웹소켓, Kafka 등)
    ├── exception           # 예외 처리
    └── util                # 유틸리티 클래스
```

### 데이터 모델

주요 도메인 모델은 다음과 같습니다:

- **User**: 사용자 정보, 친구 목록, 친구 요청 등
- **ChatRoom**: 채팅방 정보, 참여자, 메타데이터 등
- **ChatMessage**: 메시지 내용, 상태, 반응 등
- **DomainEvent**: 시스템 내 다양한 이벤트 (메시지 전송, 상태 변경 등)

## 핵심 기능 구현

### 인증 시스템

JWT 기반의 인증 시스템을 구현하여 사용자 인증 및 권한 관리를 처리합니다.

#### 주요 컴포넌트
- **JwtProvider**: 토큰 생성, 유효성 검증, 사용자 정보 추출
- **JwtAuthFilter**: 요청의 JWT 토큰 검증 및 인증 처리
- **CustomUserDetailsService**: 사용자 정보 로드 및 인증 객체 생성

#### 인증 흐름
1. 사용자 로그인 시 access token과 refresh token 발급
2. 모든 API 요청에 Authorization 헤더로 토큰 포함
3. 토큰 만료 시 refresh token으로 새 access token 발급
4. WebSocket 및 SSE 연결 시에도 JWT 인증 적용

```kotlin
// JWT 토큰 생성 예시
fun generateToken(
    id: String,
    username: String,
    expiresInMillis: Long = 3600_000
): String {
    val now = Date()
    val expiryDate = Date(now.time + expiresInMillis)

    return Jwts.builder()
        .subject(id)
        .claim("username", username)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(key, Jwts.SIG.HS256)
        .compact()
}
```

### 실시간 채팅

WebSocket, Redis, Kafka를 조합하여 확장 가능한 실시간 채팅 시스템을 구현했습니다.

#### 메시지 전송 과정

1. **클라이언트 → 서버 (WebSocket)**:
   ```
   Client → /app/chat → StompChannelInterceptor(인증, 권한 체크) → MessageStompHandler
   ```

2. **즉시 전달 (Redis Pub/Sub)**:
   ```
   MessageStompHandler → RedisTemplate.publish("chat:room:{roomId}", message) → RedisMessageListener → SimpMessagingTemplate.send("/topic/messages/{roomId}", message)
   ```

3. **영구 저장 (Kafka)**:
   ```
   MessageStompHandler → KafkaMessagePublishPort → 'chat-messages' topic → MessageKafkaConsumer → ProcessMessageUseCase → MongoDB
   ```

4. **상태 업데이트 (WebSocket)**:
   ```
   MessageKafkaConsumer → SimpMessagingTemplate.send("/topic/message/status/{roomId}", statusUpdate)
   ```

#### 주요 특징

- **메시지 임시 ID**: 클라이언트는 메시지 전송 시 임시 ID를 생성하여 상태 추적
- **상태 추적**: 메시지 상태는 `SENDING` → `SENT_TO_KAFKA` → `SAVED`로 변화
- **실패 처리**: 메시지 전송 실패 시 상태 업데이트 및 알림
- **메시지 브로드캐스트**: Redis를 통해 모든 서버에 메시지 전달

```kotlin
// 메시지 상태 업데이트 예시
private fun sendStatusUpdate(
    roomId: String,
    tempId: String,
    status: String,
    persistedId: String?,
    errorMessage: String? = null
) {
    val statusUpdate = MessageStatusResponse(tempId, status, persistedId, errorMessage)
    messagingTemplate.convertAndSend("/topic/message/status/$roomId", statusUpdate)
}
```

### 타이핑 인디케이터

사용자가 메시지를 작성 중임을 실시간으로 표시하는 기능입니다. 이는 WebSocket을 이용하여 구현되었습니다.

#### 구현 방식

1. **클라이언트 → 서버**:
   ```
   Client → /app/typing → TypingStompHandler
   ```

2. **서버 → 클라이언트**:
   ```
   TypingStompHandler → SimpMessagingTemplate.send("/topic/typing/{roomId}", typingEvent)
   ```

3. **속도 제한 적용**:
   ```kotlin
   // 타이핑 이벤트 처리 (1초에 한 번만 처리)
   @MessageMapping("/typing")
   fun handleTypingIndicator(message: TypingIndicatorMessage) {
       val key = "${message.userId}:${message.roomId}"
       val now = System.currentTimeMillis()
       val lastSent = typingRateLimiter.getOrDefault(key, 0L)

       if (now - lastSent > 1000) { // 1초 제한
           messagingTemplate.convertAndSend("/topic/typing/${message.roomId}", message)
           typingRateLimiter[key] = now
       }
   }
   ```

#### 사용자 활동 상태 추적

Redis를 이용하여 사용자의 채팅방 활성 상태를 추적합니다.

```kotlin
// 사용자 활동 상태 업데이트
@MessageMapping("/active")
fun handleActivity(message: String) {
    val activity = objectMapper.readValue(message, ChatActivity::class.java)
    val key = "active:${activity.userId}:${activity.roomId}"
    
    // Redis에 45초 만료 시간으로 저장
    redisTemplate.opsForValue().set(key, activity.active.toString(), 45, TimeUnit.SECONDS)
}
```

### 메시지 읽음 처리

메시지 읽음 상태를 추적하고 표시하는 기능입니다. 두 가지 방식으로 구현되었습니다.

#### 1. 개별 메시지 읽음 처리 (WebSocket)

```kotlin
@MessageMapping("/read")
fun handleRead(request: ChatReadRequest) {
    // 메시지 읽음 처리
    val updatedMessage = processMessageUseCase.markMessageAsRead(request.messageId, request.userId)

    // 웹소켓으로 읽음 상태 업데이트 전송
    messagingTemplate.convertAndSend("/topic/messages/${updatedMessage.roomId}", updatedMessage)
}
```

#### 2. 채팅방 전체 읽음 처리 (REST API)

```kotlin
@PostMapping("/mark-read")
fun markMessageRead(
    @RequestParam roomId: String,
    @RequestParam userId: String,
    @RequestParam(required = false) requestId: String?
): ResponseDto<Unit> {
    processMessageUseCase.markAllMessagesAsRead(roomId, userId, requestId)
    return ResponseDto.success(Unit, "메시지가 읽음으로 처리되었습니다.")
}
```

#### 읽음 상태 업데이트 처리

MongoDB에 메시지의 `readBy` 맵을 업데이트하고, 채팅방의 `unreadCount`를 조정합니다. 그리고 SSE를 통해 클라이언트에 변경 사항을 알립니다.

```kotlin
// 읽지 않은 메시지 수 업데이트 이벤트 발행
eventPublisher.publish(
    ChatUnreadCountUpdatedEvent(
        roomId = roomId.toString(),
        unreadCounts = unreadCounts,
        lastMessage = lastMessage
    )
)
```

### 친구 관리 및 추천 시스템

친구 추가, 요청 관리 및 추천 시스템을 구현했습니다.

#### 친구 기능

- **친구 요청 전송/수락/거절**
- **친구 목록 조회**
- **유저 코드를 통한 친구 찾기**

#### BFS 기반 친구 추천 시스템

MongoDB Aggregation Framework를 사용하여 소셜 네트워크 기반 친구 추천 알고리즘을 구현했습니다.

```kotlin
// BFS 기반 친구 추천 (Aggregation Pipeline 일부)
val graphLookupStage = GraphLookupOperation.builder()
    .from("users")
    .startWith("\$friends")
    .connectFrom("friends")
    .connectTo("_id")
    .maxDepth(maxDepth.toLong())
    .depthField("depth")
    .`as`("network")
```

#### 성능 최적화

- **캐싱**: Redis를 이용한 친구 추천 결과 캐싱
- **주기적 사전 계산**: 인기 사용자에 대한 추천 사전 계산
- **결과 페이징**: 대량 데이터 효율적 처리

### 채팅방 관리

채팅방 생성, 참여자 관리, 고정(즐겨찾기) 등의 기능을 구현했습니다.

#### 주요 기능

- **1:1 채팅방 생성**
- **채팅방 목록 조회**
- **채팅방 검색**
- **즐겨찾기(고정) 기능**

#### 채팅방 즐겨찾기 기능

```kotlin
// 채팅방 즐겨찾기 상태 업데이트
override fun updateFavoriteStatus(
    roomId: String,
    userId: String,
    isFavorite: Boolean
): ChatRoom {
    val chatRoom = loadChatRoomPort.findById(roomId.toObjectId())
        ?: throw ResourceNotFoundException("채팅방을 찾을 수 없습니다.")

    // 현재 핀 개수 확인 및 제한
    val pinnedRooms = loadUserPinnedRoomsPort.findByUserId(userId)
    if (isFavorite && pinnedRooms.size >= MAX_PINNED && 
        !chatRoom.metadata.participantsMetadata[userId.toObjectId()]?.isPinned!!) {
        throw IllegalStateException("최대 핀 채팅방 개수를 초과했습니다.")
    }

    // 참여자 메타데이터 업데이트
    val updatedParticipants = chatRoom.metadata.participantsMetadata.mapValues { (id, participant) ->
        if (id == userId.toObjectId()) {
            participant.copy(
                isPinned = isFavorite,
                pinTimestamp = if (isFavorite) Instant.now() else null
            )
        } else participant
    }

    // 채팅방 업데이트 및 저장
    val updatedChatRoom = chatRoom.copy(
        metadata = chatRoom.metadata.copy(participantsMetadata = updatedParticipants)
    )
    return saveChatRoomPort.save(updatedChatRoom)
}
```

## 데이터 흐름

Shoot 애플리케이션의 주요 데이터 흐름은 다음과 같습니다.

### 1. 메시지 전송 흐름

```
[클라이언트] ──WebSocket─→ [서버] ───Redis PubSub──→ [다른 클라이언트들]
                            │
                            └─────Kafka─────→ [메시지 처리기] ───→ [MongoDB]
                                                                    │
                                                  [Status Update] ←─┘
```

### 2. 세션 기반 실시간 알림 (SSE)

```
[채팅방 변경] ──→ [이벤트 생성] ──→ [이벤트 발행] ──→ [SseEmitter] ──→ [브라우저]
```

### 3. 웹소켓 연결 과정

```
[클라이언트] ─────────────────────→ [/ws/chat 엔드포인트]
       │                                   │
       └─JWT 토큰─→ [AuthHandshakeInterceptor] ──검증 성공──→ [WebSocket 연결 수립]
                           │
                           └──검증 실패──→ [연결 거부]
```

### 4. 활동 상태 관리

```
[클라이언트] ──→ [/app/active] ──→ [Redis] ──(TTL: 45초)─→ [자동 만료]
       ↑                               │
       └─────────────────────StatusAPI─┘
```

## 확장성 및 고가용성

Shoot은 대규모 사용자와 메시지 처리를 위한 확장성을 고려하여 설계되었습니다.

### 수평 확장성

- **스테이트리스 서버**: JWT 인증으로 세션 의존성 제거
- **메시지 브로커 분리**: Redis와 Kafka를 통한 메시지 전달 및 처리
- **분산 캐시**: Redis를 이용한 상태 공유

### 고가용성

- **메시지 안정성**: Kafka를 통한 안정적인 메시지 영구 저장
- **장애 복구**: Redis Pub/Sub 실패 시 Kafka 백업 메커니즘
- **데이터 지속성**: MongoDB 레플리카셋 지원

### 성능 최적화

- **인덱싱**: MongoDB 최적화된 인덱스 설계
- **캐싱 전략**: 자주 사용되는 데이터 Redis 캐싱
- **비동기 처리**: 메시지 전송과 저장의 분리

## 개발 환경 설정

### 필수 요구사항

- JDK 21
- Gradle 8.11.1 이상
- MongoDB 5.0 이상
- Redis 7.2 이상
- Kafka 3.5 이상 (선택적)

### Docker Compose 설정

프로젝트에 포함된 `docker-compose.yml`을 통해 필요한 인프라를 쉽게 구축할 수 있습니다.

```bash
# MongoDB, Redis 등 인프라 실행
docker-compose up -d

# 애플리케이션 빌드 및 실행
./gradlew bootRun
```

### 주요 설정 파일

- **application.yml**: Spring Boot 주요 설정
- **docker-compose.yml**: 개발 환경 인프라 설정
- **redis/redis.conf**: Redis 서버 설정

## 향후 개선 사항

### 에러 처리 및 복구

- 일관된 에러 처리 로직 구현
- 네트워크 불안정 환경에서의 재시도 전략 보완
- 상세한 에러 메시지와 가이드 제공

### 성능 및 최적화

- MongoDB 쿼리 최적화 (인덱스 활용, 필요한 필드만 조회)
- 무한 스크롤 시 페이징 성능 개선
- 대량 메시지 조회 시 지연 로딩 구현

### 보안

- JWT 토큰 관련 보안 강화 (시크릿 키 관리, 토큰 무효화)
- WebSocket 메시지 검증 및 제한 강화
- 사용자 입력 검증 (XSS, 인젝션 방지)