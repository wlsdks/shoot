# 웹소켓 기반 실시간 채팅 애플리케이션 "Shoot"

> [DDD 아키텍처 설계 문서](docs/ddd-architecture.md) - 도메인 주도 설계(DDD) 아키텍처에 대한 상세 설명

## 목차
1. [프로젝트 개요](#프로젝트-개요)
2. [기술 스택](#기술-스택)
   - [백엔드](#백엔드)
   - [필수 요구사항](#필수-요구사항)
3. [헥사고날 아키텍처](#헥사고날-아키텍처)
   - [패키지 구조](#패키지-구조)
   - [데이터 모델](#데이터-모델)
4. [핵심 기능](#핵심-기능)
   - [JWT 기반 인증 시스템](#jwt-기반-인증-시스템)
   - [WebSocket을 활용한 실시간 채팅](#websocket을-활용한-실시간-채팅)
   - [Redis Stream으로 메시지 브로드캐스팅](#redis-stream으로-메시지-브로드캐스팅)
   - [Kafka를 통한 메시지 영구 저장](#kafka를-통한-메시지-영구-저장)
   - [Redis 기반 분산락을 통한 동시성 제어](#redis-기반-분산락을-통한-동시성-제어)
   - [읽음 처리 및 안읽은 메시지 카운트](#읽음-처리-및-안읽은-메시지-카운트)
   - [SSE를 이용한 실시간 채팅방 목록 업데이트](#sse를-이용한-실시간-채팅방-목록-업데이트)
   - [타이핑 인디케이터 기능](#타이핑-인디케이터-기능)
   - [BFS 기반 친구 추천 시스템](#bfs-기반-친구-추천-시스템)
   - [메시지 전달 확인 및 상태 추적](#메시지-전달-확인-및-상태-추적)
   - [메시지 포워딩 및 공유](#메시지-포워딩-및-공유)
   - [메시지 핀 기능](#메시지-핀-기능)
   - [이모티콘 반응 시스템](#이모티콘-반응-시스템)
   - [URL 미리보기](#url-미리보기)
   - [예약 메시지 전송](#예약-메시지-전송)
5. [API 엔드포인트](#api-엔드포인트)
   - [사용자 관련 API](#사용자-관련-api)
   - [채팅방 관련 API](#채팅방-관련-api)
   - [메시지 관련 API](#메시지-관련-api)
   - [WebSocket 엔드포인트](#websocket-엔드포인트)
   - [SSE 엔드포인트](#sse-엔드포인트)
6. [메시지 흐름 처리 과정](#메시지-흐름-처리-과정)
   - [메시지 송신 프로세스](#메시지-송신-프로세스)
   - [메시지 수신 프로세스](#메시지-수신-프로세스)
   - [메시지 상태 관리](#메시지-상태-관리)
7. [확장성 및 고가용성](#확장성-및-고가용성)
   - [분산 시스템 설계](#분산-시스템-설계)
   - [성능 최적화](#성능-최적화)
8. [메시지 처리 전체 흐름 및 상태 변화](#메시지-처리-전체-흐름-및-상태-변화)
9. [상태별 메시지 흐름 상세 설명](#상태별-메시지-흐름-상세-설명)
   - [메시지 전송 단계 (클라이언트 → 서버)](#메시지-전송-단계-클라이언트--서버)
   - [실시간 전달 단계 (Redis Stream)](#실시간-전달-단계-redis-stream)
   - [영구 저장 단계 (Kafka → MongoDB)](#영구-저장-단계-kafka--mongodb)
   - [클라이언트 표시 단계](#클라이언트-표시-단계)
10. [오류 처리 흐름](#오류-처리-흐름)
11. [여러 서버 인스턴스의 Redis Stream 소비자 그룹 흐름](#여러-서버-인스턴스의-redis-stream-소비자-그룹-흐름)
12. [보안 및 개인정보 보호](#보안-및-개인정보-보호)
13. [배포 및 운영](#배포-및-운영)

## 프로젝트 개요

Shoot은 Spring Boot(Kotlin)과 WebSocket 기술을 활용한 실시간 채팅 애플리케이션입니다. 헥사고날 아키텍처를 채택하여 도메인 중심의 설계를 구현했으며, Redis Stream과 Kafka를 활용해 메시지 전송의 안정성과 확장성을 보장합니다.

주요 특징으로는 다음과 같은 기능들이 있습니다:
- WebSocket을 이용한 양방향 실시간 통신
- Redis Stream을 활용한 메시지 브로드캐스팅
- Kafka를 통한 메시지 영구 저장
- 실시간 타이핑 인디케이터
- 메시지 읽음 상태 추적
- SSE(Server-Sent Events)를 통한 실시간 채팅방 목록 업데이트
- BFS 기반 친구 추천 시스템

## 기술 스택

### 백엔드
- **Spring Boot 3.4.3 (Kotlin 1.9.25)** - 애플리케이션 서버
- **Spring WebSocket** - 양방향 실시간 통신
- **Spring Security** - JWT 기반 인증 및 권한 관리
- **MongoDB** - 주 데이터베이스 (채팅방, 메시지 저장)
- **PostgreSQL** - 관계형 데이터베이스 (사용자, 친구 관계 저장)
- **Redis Stream** - 메시지 브로드캐스팅
- **Redis Cache** - 캐싱 및 실시간 상태 관리
- **Kafka** - 메시지 영구 저장 및 비동기 처리
- **Server-Sent Events(SSE)** - 실시간 채팅방 목록 업데이트
- **WebSocket STOMP** - WebSocket 메시징 프로토콜

### 필수 요구사항

- JDK 21
- Gradle 8.11.1 이상
- MongoDB 5.0 이상
- Redis 7.2 이상
- Kafka 3.5 이상

## 헥사고날 아키텍처

Shoot은 헥사고날 아키텍처(포트 및 어댑터 패턴)를 채택하여 핵심 비즈니스 로직을 외부 의존성으로부터 격리하고, 테스트 용이성을 높이며, 시스템의 확장성과 유지보수성을 개선했습니다.

### 패키지 구조

```
com.stark.shoot
├── adapter                 # 외부 시스템과의 인터페이스
│   ├── in                  # 인바운드 어댑터 (컨트롤러, 웹소켓 핸들러 등)
│   │   ├── event           # 이벤트 리스너
│   │   ├── kafka           # Kafka 소비자
│   │   ├── redis           # Redis Stream 리스너
│   │   └── web             # REST API 컨트롤러
│   │       ├── socket      # WebSocket 핸들러
│   │       └── sse         # SSE 컨트롤러
│   └── out                 # 아웃바운드 어댑터 (레포지토리, 이벤트 발행자 등)
│       ├── cache           # 캐시 어댑터
│       ├── event           # 이벤트 발행 어댑터
│       ├── kafka           # Kafka 프로듀서
│       ├── message         # 메시지 관련 어댑터
│       └── persistence     # 데이터베이스 어댑터 (MongoDB, PostgreSQL)
├── application             # 애플리케이션 로직
│   ├── filter              # 필터 및 인터셉터
│   ├── port                # 포트 정의 (인터페이스)
│   │   ├── in              # 인바운드 포트 (서비스 인터페이스)
│   │   └── out             # 아웃바운드 포트 (저장소, 메시징 인터페이스)
│   └── service             # 비즈니스 로직 구현 (유스케이스)
├── domain                  # 도메인 모델
│   ├── chat                # 채팅 관련 도메인
│   │   ├── event           # 도메인 이벤트
│   │   ├── message         # 메시지 모델
│   │   ├── room            # 채팅방 모델
│   │   ├── service         # 도메인 서비스
│   │   └── user            # 사용자 모델
│   ├── common              # 공통 도메인 모델
│   └── exception           # 도메인 예외
└── infrastructure          # 공통 인프라 설정
    ├── annotation          # 커스텀 어노테이션
    ├── aop                 # 관점 지향 프로그래밍 (AOP)
    ├── config              # 스프링 설정 (보안, 웹소켓, Kafka, Redis 등)
    ├── exception           # 예외 처리
    ├── util                # 유틸리티 클래스
    └── enumerate           # 열거형 클래스
```

### 데이터 모델

주요 도메인 모델은 다음과 같습니다:

#### User
```kotlin
data class User(
    val id: ObjectId? = null,                      // 고유 ID
    val username: String,                          // 사용자명 (로그인용)
    val nickname: String,                          // 닉네임 (표시용)
    val status: UserStatus = UserStatus.OFFLINE,   // 상태 (ONLINE, OFFLINE, BUSY, AWAY 등)
    val profileImageUrl: String? = null,           // 프로필 이미지 URL
    val lastSeenAt: Instant? = null,               // 마지막 접속 시간
    val bio: String? = null,                       // 자기소개
    val passwordHash: String? = null,              // 암호화된 비밀번호
    val isDeleted: Boolean = false,                // 탈퇴 여부
    val friends: Set<ObjectId> = emptySet(),                     // 친구 목록
    val incomingFriendRequests: Set<ObjectId> = emptySet(),      // 받은 친구 요청
    val outgoingFriendRequests: Set<ObjectId> = emptySet(),      // 보낸 친구 요청
    val userCode: String,                                 // 친구 추가용 고유 코드
    val refreshToken: String? = null,                    // 리프레시 토큰
    val refreshTokenExpiration: Instant? = null          // 리프레시 토큰 만료 시간
)
```

#### ChatRoom
```kotlin
data class ChatRoom(
    val id: String? = null,                    // 채팅방 ID
    val participants: MutableSet<ObjectId>,    // 참여자 ID 목록
    val lastMessageId: String? = null,         // 마지막 메시지 ID
    val lastMessageText: String? = null,       // 마지막 메시지 내용
    val metadata: ChatRoomMetadata,            // 채팅방 메타데이터
    val lastActiveAt: Instant = Instant.now(), // 마지막 활동 시간
    val createdAt: Instant = Instant.now(),    // 생성 시간
    val updatedAt: Instant? = null             // 업데이트 시간
)

data class ChatRoomMetadata(
    val title: String? = null,                          // 채팅방 제목
    val type: ChatRoomType,                             // 채팅방 타입 (INDIVIDUAL, GROUP)
    val participantsMetadata: Map<ObjectId, Participant>,  // 참여자별 메타데이터
    val settings: ChatRoomSettings,                     // 채팅방 설정
    val announcement: String? = null                    // 공지사항
)
```

#### ChatMessage
```kotlin
data class ChatMessage(
    val id: String? = null,                     // 메시지 ID
    val roomId: String,                         // 채팅방 ID
    val senderId: String,                       // 발신자 ID
    val content: MessageContent,                // 메시지 내용
    val status: MessageStatus,                  // 메시지 상태 (SENDING, SENT, SAVED 등)
    val threadId: String? = null,               // 스레드 ID (루트 메시지 ID)
    val replyToMessageId: String? = null,       // 답장할 메시지 ID
    val reactions: Map<String, Set<String>> = emptyMap(),  // 이모티콘 반응들
    val mentions: Set<String> = emptySet(),               // 멘션된 사용자 ID 목록
    val createdAt: Instant? = Instant.now(),    // 생성 시간
    val updatedAt: Instant? = null,            // 업데이트 시간
    val isDeleted: Boolean = false,            // 삭제 여부
    val readBy: MutableMap<String, Boolean> = mutableMapOf(),  // 읽음 상태
    var metadata: MutableMap<String, Any> = mutableMapOf(),    // 추가 메타데이터
    val isPinned: Boolean = false,             // 고정 여부
    val pinnedBy: String? = null,              // 고정한 사용자 ID
    val pinnedAt: Instant? = null              // 고정 시간
)
```

## 핵심 기능

### JWT 기반 인증 시스템

Shoot은 JWT 토큰을 사용하여 사용자 인증을 처리합니다. 토큰 기반 인증은 서버의 상태를 저장하지 않아도 되므로, 서버의 확장성을 높이고 분산 환경에서 효율적으로 동작합니다.

#### 인증 흐름
1. 사용자 로그인 시 access token과 refresh token 발급
2. 모든 API 요청에 Authorization 헤더로 토큰 포함
3. 토큰 만료 시 refresh token으로 새 access token 발급
4. WebSocket 및 SSE 연결 시에도 JWT 인증 적용

```kotlin
// JWT 토큰 생성 예시
fun generateToken(
    id: String,                 // 사용자 ID (subject 필드에 저장)
    username: String,           // 사용자명 (별도 claim으로 추가)
    expiresInMillis: Long = 3600_000  // 만료 시간 (기본 1시간)
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

### WebSocket을 활용한 실시간 채팅

Spring의 STOMP WebSocket을 사용하여 클라이언트와 서버 간 양방향 실시간 통신을 구현했습니다. 웹소켓은 HTTP 연결을 통해 초기화된 후 지속적인 양방향 통신 채널을 제공하므로, 실시간 메시지 교환에 적합합니다.

```kotlin
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws/chat")
            .addInterceptors(AuthHandshakeInterceptor(jwtAuthenticationService))
            .setHandshakeHandler(CustomHandshakeHandler())
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(longArrayOf(10000, 10000))
            .setTaskScheduler(heartbeatScheduler())

        registry.setApplicationDestinationPrefixes("/app")
    }
}
```

### Redis Stream으로 메시지 브로드캐스팅

기존의 Redis PubSub 방식에서 Redis Stream으로 변경하여 메시지 전송의 신뢰성과 처리 보장성을 강화했습니다. Redis Stream은 메시지 영구 저장, 소비자 그룹 기능, 처리 확인(ACK) 등의 기능을 제공하여 메시지 유실을 방지하고 정확한 순서를 보장합니다.

```kotlin
// 메시지를 Redis Stream에 발행
private fun publishToRedis(message: ChatMessageRequest) {
    val streamKey = "stream:chat:room:${message.roomId}"
    try {
        val messageJson = objectMapper.writeValueAsString(message)
        val map = mapOf("message" to messageJson)

        // StreamRecords를 사용한 메시지 추가
        val record = StreamRecords.newRecord()
            .ofMap(map)
            .withStreamKey(streamKey)

        // Stream에 추가
        val messageId = redisTemplate.opsForStream<String, String>()
            .add(record)

        logger.debug { "Redis Stream에 메시지 발행: $streamKey, id: $messageId" }
    } catch (e: Exception) {
        logger.error(e) { "Redis 발행 실패: ${e.message}" }
        throw e
    }
}
```

메시지 소비는 소비자 그룹을 통해 이루어지며, 주기적으로 스트림을 폴링하여 메시지를 처리합니다:

```kotlin
@Scheduled(fixedRate = 100) // 100ms마다 실행
private fun pollMessages() {
    val streamKeys = redisTemplate.keys("stream:chat:room:*")
    if (streamKeys.isEmpty()) return

    val readOptions = StreamReadOptions.empty()
        .count(10)
        .block(Duration.ofMillis(100))

    val consumerOptions = Consumer.from("chat-consumers", "consumer-1")

    for (key in streamKeys) {
        val messages = redisTemplate.opsForStream<String, Any>()
            .read(consumerOptions, readOptions, StreamOffset.create(key, ReadOffset.lastConsumed()))

        for (message in messages) {
            processMessage(message)
            redisTemplate.opsForStream<String, Any>()
                .acknowledge("chat-consumers", key, message.id)
        }
    }
}
```

### Kafka를 통한 메시지 영구 저장

메시지의 안정적인 영구 저장을 위해 Kafka를 사용합니다. Redis Stream이 실시간 메시지 전송을 담당한다면, Kafka는 메시지의 영구 저장과 비동기 처리를 담당합니다. 이를 통해 시스템 장애 시에도 메시지 손실을 방지하고, 대용량 메시지 처리가 가능합니다.

```kotlin
// Kafka로 메시지 이벤트 발행
private fun sendToKafka(message: ChatMessageRequest): CompletableFuture<Void> {
    val chatEvent = ChatEvent(
        type = EventType.MESSAGE_CREATED,
        data = chatMessage
    )

    return kafkaMessagePublishPort.publishChatEvent(
        topic = "chat-messages",
        key = message.roomId,
        event = chatEvent
    ).thenAccept {
        // Kafka 발행 성공 시 상태 업데이트
        val statusUpdate = MessageStatusResponse(
            tempId = message.tempId ?: "",
            status = MessageStatus.SENT_TO_KAFKA.name,
            persistedId = null
        )
        messagingTemplate.convertAndSend("/topic/message/status/${message.roomId}", statusUpdate)
    }
}
```

Kafka 소비자는 메시지를 데이터베이스에 저장하고, 저장 결과를 클라이언트에게 통지합니다:

```kotlin
@KafkaListener(topics = ["chat-messages"], groupId = "shoot")
fun consumeMessage(@Payload event: ChatEvent) {
    if (event.type == EventType.MESSAGE_CREATED) {
        try {
            // 임시 ID와 채팅방 ID 추출
            val tempId = event.data.metadata["tempId"] as? String ?: return
            val roomId = event.data.roomId

            // 처리 중 상태 업데이트
            sendStatusUpdate(roomId, tempId, MessageStatus.PROCESSING.name, null)

            // 메시지 저장
            val savedMessage = processMessageUseCase.processMessageCreate(event.data)

            // 저장 성공 상태 업데이트
            sendStatusUpdate(roomId, tempId, MessageStatus.SAVED.name, savedMessage.id)
        } catch (e: Exception) {
            sendErrorResponse(event, e)
        }
    }
}
```

### Redis 기반 분산락을 통한 동시성 제어

분산 환경에서 여러 서버가 동일한 데이터에 동시 접근할 때 발생하는 동시성 문제를 해결하기 위해 Redis 기반 분산락을 구현했습니다. 이 메커니즘은 메시지 처리, 채팅방 메타데이터 업데이트, 읽지 않은 메시지 카운트 처리 등에서 데이터 일관성을 보장합니다.

**핵심 구현 요소:**
- Redis의 SETNX 명령어를 활용한 원자적 락 획득
- 자동 만료 시간 설정으로 서버 장애 시에도 락 해제 보장
- Lua 스크립트를 통한 안전한 락 해제 (소유자 검증)
- 지수 백오프 전략을 적용한 효율적인 재시도 메커니즘
- 채팅방별 독립적인 락으로 시스템 병렬성 유지

**동작 방식:**
```kotlin
// 채팅 메시지 처리 시 분산락 적용 예시
override fun processMessageCreate(message: ChatMessage): ChatMessage {
   // 채팅방 ID 기반으로 락 획득
   return redisLockManager.withLock("chatroom:${message.roomId}", "processor-${UUID.randomUUID()}") {
      // 트랜잭션적 작업 수행 (메시지 저장, 메타데이터 업데이트, 이벤트 발행 등)
      // ...
   } // 작업 완료 후 자동으로 락 해제
}
```

### 읽음 처리 및 안읽은 메시지 카운트

메시지 읽음 상태를 추적하고 안읽은 메시지 수를 계산하는 기능을 제공합니다. 채팅방에 참여중인 사용자의 메시지 읽음 여부를 실시간으로 추적하고, 채팅방 목록에서 안읽은 메시지 수를 표시합니다.

```kotlin
// 메시지 읽음 처리 (WebSocket)
@MessageMapping("/read")
fun handleRead(request: ChatReadRequest) {
    // 메시지 읽음 처리
    val updatedMessage = markMessageReadUseCase.markMessageAsRead(request.messageId, request.userId)

    // 웹소켓으로 읽음 상태 업데이트 전송
    messagingTemplate.convertAndSend("/topic/messages/${updatedMessage.roomId}", updatedMessage)
}

// 채팅방 전체 읽음 처리 (REST API)
@PostMapping("/mark-read")
fun markMessageRead(
    @RequestParam roomId: String,
    @RequestParam userId: String,
    @RequestParam(required = false) requestId: String?
): ResponseDto<Unit> {
    markMessageReadUseCase.markAllMessagesAsRead(roomId, userId, requestId)
    return ResponseDto.success(Unit, "메시지가 읽음으로 처리되었습니다.")
}
```

메시지 읽음 처리 시, 채팅방의 `unreadCount`를 갱신하고 SSE를 통해 클라이언트에 변경 사항을 알립니다:

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

### SSE를 이용한 실시간 채팅방 목록 업데이트

Server-Sent Events(SSE)를 사용하여 채팅방 목록의 실시간 업데이트를 구현했습니다. 새 메시지 도착, 안읽은 메시지 수 변경, 새 채팅방 생성 등의 이벤트가 발생할 때 클라이언트에 자동으로 알림을 전송합니다.

```kotlin
@GetMapping(value = ["/updates/{userId}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
fun streamUpdates(@PathVariable userId: String): SseEmitter {
    return sseEmitterUseCase.createEmitter(userId)
}

// SSE 이미터를 통한 업데이트 전송
fun sendUpdate(userId: String, roomId: String, unreadCount: Int, lastMessage: String?) {
    emitters[userId]?.let { emitter ->
        try {
            val data = mapOf(
                "roomId" to roomId,
                "unreadCount" to unreadCount,
                "lastMessage" to (lastMessage ?: "")
            )
            emitter.send(SseEmitter.event().data(data))
        } catch (e: Exception) {
            emitters.remove(userId)
        }
    }
}
```

### 타이핑 인디케이터 기능

사용자가 메시지를 작성 중임을 실시간으로 표시하는 기능입니다. WebSocket을 통해 타이핑 상태 이벤트를 송수신하고, 속도 제한을 적용하여 서버 부하를 줄입니다.

```kotlin
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

### BFS 기반 친구 추천 시스템

MongoDB의 `$graphLookup` 연산자를 활용한 BFS(너비 우선 탐색) 알고리즘으로 소셜 네트워크 기반 친구 추천 시스템을 구현했습니다. 사용자의 친구, 친구의 친구 등 소셜 그래프를 탐색하여 추천 후보를 찾습니다.

```kotlin
// BFS 기반 친구 추천 MongoDB Aggregation 파이프라인 구현
override fun findBFSRecommendedUsers(
    userId: ObjectId,
    maxDepth: Int,
    skip: Int,
    limit: Int
): List<User> {
    // 1) 시작 사용자 매칭
    val matchStage = Aggregation.match(Criteria.where("_id").`is`(userId))

    // 2) 친구 네트워크 탐색 ($graphLookup)
    val graphLookupStage = GraphLookupOperation.builder()
        .from("users")
        .startWith("\$friends")
        .connectFrom("friends")
        .connectTo("_id")
        .maxDepth(maxDepth.toLong())
        .depthField("depth")
        .`as`("network")

    // 3) 추천 제외 대상 (자신, 이미 친구, 요청 중인 사용자)
    val addExclusionsStage = AddFieldsOperation.builder()
        .addField("exclusions")
        .withValue(Document("\$setUnion", listOf(
            "\$friends", 
            "\$incomingFriendRequests",
            "\$outgoingFriendRequests", 
            listOf("\$_id")
        )))
        .build()

    // 4) 필터링, 상호 친구 수 계산, 정렬 및 페이징 단계
    // ...

    // 최종 Aggregation 파이프라인 실행
    val results = mongoTemplate.aggregate(aggregation, "users", UserDocument::class.java)
    return results.mappedResults.map { userMapper.toDomain(it) }
}
```

성능 최적화를 위해 Redis를 활용한 캐싱, 주기적 사전 계산, 결과 페이징 등의 기법을 적용했습니다.

### 메시지 전달 확인 및 상태 추적

메시지가 전송되고 처리되는 전체 과정을 추적하여 사용자에게 현재 상태를 실시간으로 제공합니다. 각 메시지는 고유한 임시 ID를 가지고 있어 클라이언트에서 서버까지의 전체 여정을 추적할 수 있습니다.

```kotlin
// 메시지 상태 업데이트 전송
private fun sendStatusUpdate(
    roomId: String,
    tempId: String,
    status: String,
    persistedId: String?,
    errorMessage: String? = null
) {
    val statusUpdate = MessageStatusResponse(
        tempId = tempId,
        status = status,
        persistedId = persistedId,
        errorMessage = errorMessage
    )
    messagingTemplate.convertAndSend("/topic/message/status/$roomId", statusUpdate)
}
```

메시지 상태는 다음과 같은 단계로 추적됩니다:
1. **SENDING**: 클라이언트에서 서버로 전송 중
2. **SENT_TO_KAFKA**: Redis Stream을 통해 전달되고 Kafka로 발행됨
3. **PROCESSING**: Kafka 소비자가 메시지 처리 중
4. **SAVED**: MongoDB에 영구 저장됨
5. **FAILED**: 처리 중 오류 발생

### 메시지 포워딩 및 공유

사용자가 메시지를 다른 채팅방으로 전달하거나 여러 채팅방에 동시에 공유할 수 있는 기능을 제공합니다.

```kotlin
@PostMapping("/forward")
fun forwardMessage(
    @RequestBody request: MessageForwardRequest,
    authentication: Authentication
): ResponseDto<List<MessageResponse>> {
    val userId = authentication.name.toLong()
    val forwardedMessages = messageForwardUseCase.forwardMessage(
        userId = userId,
        messageId = request.messageId,
        targetRoomIds = request.targetRoomIds,
        additionalContent = request.additionalContent
    )
    return ResponseDto.success(forwardedMessages.map { it.toResponse() })
}
```

메시지 포워딩 시 원본 메시지의 참조를 유지하여 출처를 추적할 수 있으며, 추가 코멘트를 포함할 수 있습니다.

### 메시지 핀 기능

중요한 메시지를 채팅방 상단에 고정할 수 있는 핀 기능을 제공합니다. 공지사항, 중요 정보, 자주 참조하는 메시지 등을 쉽게 접근할 수 있도록 합니다.

```kotlin
@PostMapping("/pin")
fun pinMessage(
    @RequestBody request: PinMessageRequest,
    authentication: Authentication
): ResponseDto<MessageResponse> {
    val userId = authentication.name.toLong()
    val pinnedMessage = messagePinUseCase.pinMessage(
        messageId = request.messageId,
        roomId = request.roomId,
        userId = userId
    )
    return ResponseDto.success(pinnedMessage.toResponse())
}
```

핀 기능은 다음과 같은 특징을 가집니다:
- 채팅방별로 최대 3개까지 메시지 고정 가능
- 메시지를 고정한 사용자 정보 및 시간 기록
- 관리자 권한이 있는 사용자만 핀/언핀 가능
- 실시간으로 모든 참여자에게 핀 상태 변경 알림

### 이모티콘 반응 시스템

메시지에 다양한 이모티콘으로 반응할 수 있는 기능을 제공합니다. 텍스트 응답 없이도 감정이나 의견을 빠르게 표현할 수 있습니다.

```kotlin
@PostMapping("/reaction")
fun addReaction(
    @RequestBody request: AddReactionRequest,
    authentication: Authentication
): ResponseDto<MessageResponse> {
    val userId = authentication.name.toLong()
    val updatedMessage = messageReactionUseCase.addReaction(
        messageId = request.messageId,
        userId = userId,
        reaction = request.reaction
    )
    return ResponseDto.success(updatedMessage.toResponse())
}
```

이모티콘 반응 시스템의 특징:
- 메시지당 다양한 이모티콘 지원
- 각 이모티콘별 반응한 사용자 목록 제공
- 실시간 업데이트로 모든 참여자에게 반응 상태 공유
- 반응 추가/제거 기능

### URL 미리보기

메시지에 포함된 URL을 자동으로 감지하여 해당 웹페이지의 미리보기를 생성합니다. 제목, 설명, 대표 이미지 등을 추출하여 메시지와 함께 표시합니다.

```kotlin
// URL 미리보기 생성
private fun generateUrlPreview(url: String): UrlPreview {
    return try {
        val document = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(5000)
            .get()

        val title = document.select("meta[property=og:title]").attr("content") 
            ?: document.title()
        val description = document.select("meta[property=og:description]").attr("content") 
            ?: document.select("meta[name=description]").attr("content")
        val imageUrl = document.select("meta[property=og:image]").attr("content")

        UrlPreview(url, title, description, imageUrl)
    } catch (e: Exception) {
        logger.error(e) { "URL 미리보기 생성 실패: $url" }
        UrlPreview(url, url, null, null)
    }
}
```

URL 미리보기 기능의 특징:
- 메시지 전송 시 URL 자동 감지
- Open Graph 태그 및 메타 태그를 활용한 정보 추출
- 캐싱을 통한 성능 최적화
- 다양한 웹사이트 지원

### 예약 메시지 전송

특정 시간에 자동으로 전송되는 예약 메시지 기능을 제공합니다. 중요한 알림, 기념일 축하, 정기 공지 등을 미리 작성하여 예약할 수 있습니다.

```kotlin
@PostMapping("/schedule")
fun scheduleMessage(
    @RequestBody request: ScheduleMessageRequest,
    authentication: Authentication
): ResponseDto<ScheduledMessageResponse> {
    val userId = authentication.name.toLong()
    val scheduledMessage = scheduleMessageUseCase.scheduleMessage(
        userId = userId,
        roomId = request.roomId,
        content = request.content,
        scheduledAt = request.scheduledAt
    )
    return ResponseDto.success(scheduledMessage.toResponse())
}
```

예약 메시지 기능의 특징:
- 정확한 시간에 메시지 전송 보장
- 예약 메시지 목록 조회 및 관리
- 예약 취소 및 수정 기능
- 반복 예약 지원 (매일, 매주, 매월)

## API 엔드포인트

### 사용자 관련 API

| 엔드포인트 | 메소드 | 설명 | 인증 필요 |
|------------|--------|------|-----------|
| `/api/v1/auth/signup` | POST | 회원가입 | 아니오 |
| `/api/v1/auth/login` | POST | 로그인 | 아니오 |
| `/api/v1/auth/refresh` | POST | 토큰 갱신 | 아니오 |
| `/api/v1/users/me` | GET | 내 프로필 조회 | 예 |
| `/api/v1/users/me` | PUT | 프로필 수정 | 예 |
| `/api/v1/users/me/profile-image` | PUT | 프로필 이미지 설정 | 예 |
| `/api/v1/users/me/background-image` | PUT | 배경 이미지 설정 | 예 |
| `/api/v1/users/{userId}` | GET | 특정 사용자 프로필 조회 | 예 |
| `/api/v1/users/status` | PUT | 상태 업데이트 | 예 |
| `/api/v1/users/search` | GET | 사용자 검색 | 예 |
| `/api/v1/users/friends` | GET | 친구 목록 조회 | 예 |
| `/api/v1/users/friends/requests` | GET | 친구 요청 목록 조회 | 예 |
| `/api/v1/users/friends/requests` | POST | 친구 요청 보내기 | 예 |
| `/api/v1/users/friends/requests/{requestId}/accept` | POST | 친구 요청 수락 | 예 |
| `/api/v1/users/friends/requests/{requestId}/reject` | POST | 친구 요청 거절 | 예 |
| `/api/v1/users/friends/{friendId}` | DELETE | 친구 삭제 | 예 |
| `/api/v1/users/recommendations` | GET | 친구 추천 목록 | 예 |

### 채팅방 관련 API

| 엔드포인트 | 메소드 | 설명 | 인증 필요 |
|------------|--------|------|-----------|
| `/api/v1/chatrooms` | GET | 채팅방 목록 조회 | 예 |
| `/api/v1/chatrooms` | POST | 채팅방 생성 | 예 |
| `/api/v1/chatrooms/{roomId}` | GET | 채팅방 상세 조회 | 예 |
| `/api/v1/chatrooms/{roomId}` | PUT | 채팅방 정보 수정 | 예 |
| `/api/v1/chatrooms/{roomId}` | DELETE | 채팅방 나가기/삭제 | 예 |
| `/api/v1/chatrooms/{roomId}/participants` | GET | 참여자 목록 조회 | 예 |
| `/api/v1/chatrooms/{roomId}/participants` | POST | 참여자 추가 | 예 |
| `/api/v1/chatrooms/{roomId}/participants/{userId}` | DELETE | 참여자 제거 | 예 |
| `/api/v1/chatrooms/{roomId}/notice` | GET | 공지사항 조회 | 예 |
| `/api/v1/chatrooms/{roomId}/notice` | POST | 공지사항 등록 | 예 |
| `/api/v1/chatrooms/favorites` | GET | 즐겨찾기 채팅방 목록 | 예 |
| `/api/v1/chatrooms/{roomId}/favorite` | POST | 즐겨찾기 추가 | 예 |
| `/api/v1/chatrooms/{roomId}/favorite` | DELETE | 즐겨찾기 제거 | 예 |
| `/api/v1/chatrooms/search` | GET | 채팅방 검색 | 예 |
| `/api/v1/chatrooms/multiple` | POST | 다중 채팅방 생성 | 예 |

### 메시지 관련 API

| 엔드포인트 | 메소드 | 설명 | 인증 필요 |
|------------|--------|------|-----------|
| `/api/v1/messages/{roomId}` | GET | 메시지 목록 조회 | 예 |
| `/api/v1/messages/{messageId}` | GET | 메시지 상세 조회 | 예 |
| `/api/v1/messages/{messageId}` | PUT | 메시지 수정 | 예 |
| `/api/v1/messages/{messageId}` | DELETE | 메시지 삭제 | 예 |
| `/api/v1/messages/mark-read` | POST | 메시지 읽음 처리 | 예 |
| `/api/v1/messages/forward` | POST | 메시지 전달 | 예 |
| `/api/v1/messages/pin` | POST | 메시지 고정 | 예 |
| `/api/v1/messages/pin/{messageId}` | DELETE | 메시지 고정 해제 | 예 |
| `/api/v1/messages/pins/{roomId}` | GET | 고정된 메시지 목록 | 예 |
| `/api/v1/messages/thread` | GET | 스레드 메시지 조회 | 예 |
| `/api/v1/messages/reaction` | POST | 이모티콘 반응 추가 | 예 |
| `/api/v1/messages/reaction` | DELETE | 이모티콘 반응 제거 | 예 |
| `/api/v1/messages/schedule` | POST | 메시지 예약 | 예 |
| `/api/v1/messages/schedule` | GET | 예약 메시지 목록 | 예 |
| `/api/v1/messages/schedule/{scheduleId}` | DELETE | 예약 메시지 취소 | 예 |

### WebSocket 엔드포인트

| 엔드포인트 | 설명 |
|------------|------|
| `/ws/chat` | WebSocket 연결 엔드포인트 |
| `/app/chat` | 메시지 전송 |
| `/app/typing` | 타이핑 인디케이터 |
| `/app/read` | 메시지 읽음 처리 |
| `/topic/messages/{roomId}` | 채팅방 메시지 구독 |
| `/topic/message/status/{roomId}` | 메시지 상태 업데이트 구독 |
| `/topic/typing/{roomId}` | 타이핑 인디케이터 구독 |
| `/topic/active/{roomId}` | 활성 사용자 상태 구독 |

### SSE 엔드포인트

| 엔드포인트 | 설명 |
|------------|------|
| `/api/v1/sse/updates/{userId}` | 채팅방 목록 업데이트 스트림 |
| `/api/v1/sse/unread/{userId}` | 안읽은 메시지 카운트 스트림 |
| `/api/v1/sse/read-count/{roomId}/{messageId}` | 메시지 읽음 카운트 스트림 |

## 보안 및 개인정보 보호

Shoot은 사용자 데이터 보호와 시스템 보안을 위해 다양한 보안 메커니즘을 구현하고 있습니다:

### 인증 및 권한 관리
- JWT 기반 토큰 인증으로 안전한 API 접근 제어
- 토큰 만료 및 갱신 메커니즘으로 보안 강화
- 역할 기반 접근 제어(RBAC)로 권한별 기능 제한
- WebSocket 및 SSE 연결에 대한 인증 적용

### 데이터 보안
- 비밀번호 bcrypt 해싱으로 안전하게 저장
- 민감한 정보 전송 시 암호화 적용
- 개인식별정보(PII) 접근 제한 및 로깅
- 메시지 내용 저장 시 암호화 옵션 제공

### 보안 모니터링 및 대응
- 로그인 시도 제한으로 무차별 대입 공격 방지
- 비정상 접근 패턴 감지 및 차단
- 보안 이벤트 로깅 및 모니터링
- 취약점 정기 점검 및 패치 적용

## 배포 및 운영

### 배포 환경
- Docker 컨테이너화로 일관된 환경 제공
- Kubernetes 기반 오케스트레이션으로 확장성 확보
- CI/CD 파이프라인을 통한 자동화된 빌드 및 배포
- 멀티 리전 배포로 지역별 지연 시간 최소화

### 모니터링 및 로깅
- Prometheus와 Grafana를 활용한 실시간 모니터링
- ELK 스택으로 중앙화된 로그 관리
- 알림 시스템으로 이상 징후 즉시 감지
- 성능 지표 수집 및 분석

### 장애 대응
- 자동 복구 메커니즘 구현
- 데이터 백업 및 복구 전략
- 장애 시나리오별 대응 절차 문서화
- 정기적인 재해 복구 훈련

### 확장 전략
- 수평적 확장을 통한 부하 분산
- 데이터베이스 샤딩 및 레플리케이션
- 캐싱 계층 최적화
- 리소스 사용량 기반 자동 스케일링

## 메시지 흐름 처리 과정

### 메시지 송신 프로세스

메시지가 클라이언트에서 서버로 전송되는 과정은 다음과 같습니다:

1. **클라이언트 → 서버 (WebSocket)**
   ```
   Client → /app/chat → StompChannelInterceptor(인증, 권한 체크) → MessageStompHandler
   ```

2. **즉시 전달 (Redis Stream)**
   ```
   MessageStompHandler → Redis Stream 발행 → RedisStreamListener → SimpMessagingTemplate.send("/topic/messages/{roomId}", message)
   ```

3. **영구 저장 (Kafka)**
   ```
   MessageStompHandler → KafkaMessagePublishPort → 'chat-messages' topic → MessageKafkaConsumer → ProcessMessageUseCase → MongoDB
   ```

4. **상태 업데이트 (WebSocket)**
   ```
   MessageKafkaConsumer → SimpMessagingTemplate.send("/topic/message/status/{roomId}", statusUpdate)
   ```

### 메시지 수신 프로세스

1. **Redis Stream 구독**:
   - Redis Stream을 주기적으로 폴링하여 새 메시지 확인
   - 여러 서버 인스턴스가 소비자 그룹을 통해 메시지 수신
   - 읽은 메시지는 ACK 처리로 중복 처리 방지

2. **WebSocket 브로드캐스팅**:
   - 수신한 메시지를 WebSocket을 통해 채팅방 참여자에게 브로드캐스팅
   - 타겟 경로: `/topic/messages/{roomId}`

3. **메시지 상태 처리**:
   - Kafka 컨슈머에서 메시지 저장 후 상태 업데이트 전송
   - 타겟 경로: `/topic/message/status/{roomId}`

4. **읽음 상태 업데이트**:
   - 메시지를 읽었을 때 서버에서 readBy 필드 업데이트
   - 타겟 경로: `/topic/messages/{roomId}`

### 메시지 상태 관리

메시지는 다음과 같은 상태를 거치며 처리됩니다:

1. **SENDING**: 클라이언트에서 전송 중인 상태
2. **SENT_TO_KAFKA**: Redis Stream을 통해 전송되고 Kafka로 발행된 상태
3. **PROCESSING**: Kafka 소비자가 메시지 저장을 시작한 상태
4. **SAVED**: MongoDB에 성공적으로 저장된 상태
5. **FAILED**: 처리 중 오류가 발생한 상태

각 상태 변경 시 클라이언트에 상태 업데이트 이벤트를 전송하여 UI 업데이트가 가능하게 합니다:

```kotlin
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

## 확장성 및 고가용성

### 분산 시스템 설계

Shoot은 대규모 사용자와 메시지 처리를 위한 분산 시스템으로 설계되었습니다:

1. **스테이트리스 서버**:
   - JWT 기반 인증으로 서버가 상태를 유지할 필요가 없음
   - 인증된 요청은 어떤 서버 인스턴스에서도 처리 가능

2. **메시지 브로커 분리**:
   - Redis Stream과 Kafka를 통한 메시지 전달 및 처리
   - 서버 간 메시지 동기화 자동 처리

3. **소비자 그룹 활용**:
   - Redis Stream의 소비자 그룹 기능으로 메시지 분산 처리
   - 각 서버 인스턴스가 특정 메시지를 담당하여 중복 처리 방지

4. **샤딩 및 파티셔닝**:
   - Kafka 토픽의 채팅방 ID 기반 파티셔닝으로 메시지 순서 보장
   - MongoDB 컬렉션 샤딩으로 데이터 분산 저장

### 성능 최적화

대규모 트래픽과 데이터 처리를 위한 성능 최적화 전략:

1. **인덱싱 전략**:
   - MongoDB 인덱스 최적화로 쿼리 성능 향상
   - 복합 인덱스와 부분 인덱스를 활용한 맞춤형 인덱싱

2. **캐싱 계층**:
   - Redis를 활용한 다단계 캐싱 전략
   - 자주 접근하는 데이터(채팅방 목록, 친구 추천 등) 캐싱

3. **비동기 처리**:
   - 메시지 전송과 저장의 분리로 응답 시간 최소화
   - 비동기 이벤트 기반 아키텍처로 시스템 부하 분산

4. **커넥션 관리**:
   - WebSocket 커넥션 풀링과 하트비트로 연결 관리
   - SSE 타임아웃 및 재연결 메커니즘

5. **속도 제한(Rate Limiting)**:
   - 사용자별, 채팅방별 메시지 전송 속도 제한
   - 타이핑 인디케이터 등 빈번한 이벤트 제한

```kotlin
// WebSocket 인바운드 채널 설정 및 속도 제한
override fun configureClientInboundChannel(registration: ChannelRegistration) {
    registration.interceptors(
        StompChannelInterceptor(loadChatRoomPort, findUserPort, objectMapper),
        rateLimitInterceptor
    )

    registration.taskExecutor()
        .corePoolSize(8)
        .maxPoolSize(20)
        .queueCapacity(100)
}
```

## 메시지 처리 전체 흐름 및 상태 변화

```
[클라이언트]───────────────────────────────────────────────┐
     │                                                  │
     │ 1. 메시지 전송 (WebSocket)                          │
     ↓                                                  │
[서버 (MessageStompHandler)]                             │
     │                                                  │
     ├─────────────────────┬─────────────────────┐      │
     │                     │                     │      │
     │                     │                     │      │
     ↓                     ↓                     ↓      │
 2. 상태: SENDING   3. Redis Stream 발행    4. Kafka 발행  │
     │                     │                     │      │
     │                     │                     │      │
     │                     │                     │      │
     │                     ↓                     ↓      │
     │          5. Stream Consumer 수신  6. Kafka Consumer 수신
     │                     │                     │      │
     │                     │                     │      │
     │                     │                     │      │
     │                     ↓                     ↓      │
     │           7. 웹소켓으로 메시지 전달   8. 상태: SENT_TO_KAFKA
     │                     │                     │      │
     │                     │                     │      │
     │                     │                     │      │
     │                     ↓                     ↓      │
     │              [다른 클라이언트들]      9. 상태: PROCESSING
     │                                           │      │
     │                                           │      │
     │                                           ↓      │
     │                                  10. MongoDB 저장 │
     │                                           │      │
     │                                           │      │
     │                                           ↓      │
     │                                   11. 상태: SAVED │
     │                                           │      │
     │                                           │      │
     └──────────────────←─────────────────←──────┘      │
                                                        │
     12. 상태 업데이트 화면에 표시                            │
      (SENDING → SENT_TO_KAFKA → PROCESSING → SAVED)    │
                                                        │
     └──────────────────←─────────────────←─────────────┘
```

## 상태별 메시지 흐름 상세 설명

### 메시지 전송 단계 (클라이언트 → 서버)

**1. 메시지 전송 (WebSocket)**
- 클라이언트가 `/app/chat` 엔드포인트로 메시지 전송
- 임시 ID(tempId) 생성하여 클라이언트에서 메시지 추적 시작

**2. 상태: SENDING**
- 서버에서 메시지 수신 즉시 상태를 SENDING으로 설정
- 클라이언트에게 WebSocket으로 상태 업데이트 전송 (`/topic/message/status/{roomId}`)

### 실시간 전달 단계 (Redis Stream)

**3. Redis Stream 발행**
- 메시지를 Redis Stream에 발행 (`stream:chat:room:{roomId}`)
- 실시간 메시지 전달을 위한 첫 번째 경로

**5. Stream Consumer 수신**
- 서버(들)의 Stream 소비자가 메시지 수신
- 여러 서버 인스턴스가 소비자 그룹을 통해 메시지 분산 처리

**7. 웹소켓으로 메시지 전달**
- 수신한 메시지를 WebSocket을 통해 채팅방의 다른 클라이언트들에게 전달
- 목적지: `/topic/messages/{roomId}`

### 영구 저장 단계 (Kafka → MongoDB)

**4. Kafka 발행**
- 메시지를 Kafka 토픽 'chat-messages'에 발행
- 영구 저장을 위한 두 번째 경로

**6. Kafka Consumer 수신 & 8. 상태: SENT_TO_KAFKA**
- Kafka 컨슈머가 메시지 수신
- 메시지 상태를 SENT_TO_KAFKA로 업데이트
- WebSocket을 통해 상태 업데이트 전송 (`/topic/message/status/{roomId}`)

**9. 상태: PROCESSING**
- MongoDB 저장 시작 전 상태 업데이트
- WebSocket을 통해 PROCESSING 상태 전송

**10. MongoDB 저장**
- 메시지를 MongoDB에 영구 저장
- 임시 ID를 영구 ID로 대체

**11. 상태: SAVED**
- 저장 완료 후 상태를 SAVED로 업데이트
- 영구 메시지 ID와 함께 상태 업데이트 전송
- WebSocket 경로: `/topic/message/status/{roomId}`

### 클라이언트 표시 단계

**12. 상태 업데이트 화면에 표시**
- 클라이언트는 메시지의 상태 변화에 따라 UI 업데이트
- 임시 ID로 메시지를 추적하다가 영구 ID로 대체
- 상태 흐름: SENDING → SENT_TO_KAFKA → PROCESSING → SAVED

## 오류 처리 흐름

```
            [메시지 처리 중 오류 발생]
                      │
                      ↓
              [상태: FAILED 설정]
                      │
                      ↓
        [오류 메시지와 함께 상태 업데이트 전송]
                      │
                      ↓
       [클라이언트에서 오류 표시 및 재시도 옵션]
```

## 여러 서버 인스턴스의 Redis Stream 소비자 그룹 흐름

```
                          [Redis Stream]
                                │
                                ↓
           ┌──────────────────────────────────────────┐
           │                                          │
           ↓                                          ↓
     [서버 인스턴스 A]                             [서버 인스턴스 B]
[소비자 그룹: chat-consumers]                [소비자 그룹: chat-consumers]
[소비자 ID: consumer-uuid1]                 [소비자 ID: consumer-uuid2]
           │                                          │
           │ 메시지 1,3,5 수신                           │ 메시지 2,4,6 수신
           ↓                                          ↓
[클라이언트들에게 WebSocket 전송]              [클라이언트들에게 WebSocket 전송]
```

## 감사의 말
- Shoot 프로젝트는 다양한 오픈소스 프로젝트와 커뮤니티의 도움으로 개발되었습니다. 특히 Spring Framework, Redis, Kafka, MongoDB 팀들과 커뮤니티에 감사드립니다. 이 프로젝트가 실시간 메시징 애플리케이션 개발에 관심 있는 개발자들에게 영감이 되기를 바랍니다.
---
© 2025 Shoot Project. (Stark, wlsdks) 모든 권리 보유.
