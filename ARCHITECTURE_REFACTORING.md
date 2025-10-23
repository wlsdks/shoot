# Architecture Refactoring: Redis Stream 제거

> **작성일**: 2025-10-24
> **목표**: Kafka 단일 경로 아키텍처로 전환하여 복잡성 감소 및 확장성 확보

## 목차

1. [개요](#개요)
2. [기존 아키텍처 (Dual Publishing)](#기존-아키텍처-dual-publishing)
3. [문제점 분석](#문제점-분석)
4. [의사결정 과정](#의사결정-과정)
5. [최종 아키텍처 (Kafka Only)](#최종-아키텍처-kafka-only)
6. [제거된 컴포넌트](#제거된-컴포넌트)
7. [성능 최적화](#성능-최적화)
8. [마이그레이션 체크리스트](#마이그레이션-체크리스트)

---

## 개요

### 리팩토링 배경

기존 아키텍처는 **Redis Stream**과 **Kafka**를 동시에 사용하는 Dual Publishing 패턴을 사용하고 있었습니다:

- **Redis Stream**: 초저지연 실시간 메시지 전달 (1-5ms)
- **Kafka**: 메시지 영속화 및 신뢰성 보장 (10-50ms)

### 리팩토링 목표

1. **복잡성 감소**: 두 개의 메시지 브로커를 하나로 통합
2. **운영 비용 절감**: Redis + Kafka → Kafka만 운영
3. **확장성 확보**: 10만 유저 목표에 적합한 아키텍처
4. **일관성 개선**: 단일 진실 공급원(Single Source of Truth)

---

## 기존 아키텍처 (Dual Publishing)

### 구조도

```
┌─────────────────────────────────────────────────────────────┐
│                        Client (WebSocket)                    │
└─────────────────────────┬───────────────────────────────────┘
                          │ STOMP /app/chat/send
                          ▼
                 ┌────────────────────┐
                 │ MessageStompHandler │
                 └────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │ MessagePublisherAdapter│
              └─────┬──────────┬──────┘
                    │          │
        ┌───────────┘          └────────────┐
        ▼                                   ▼
┌───────────────┐                   ┌──────────────┐
│ Redis Stream  │                   │    Kafka     │
│ (실시간 전달)  │                   │  (영속화)     │
└───────┬───────┘                   └──────┬───────┘
        │                                   │
        ▼                                   ▼
┌──────────────────────┐         ┌────────────────────┐
│MessageRedisStream    │         │MessageKafka        │
│Listener (100ms poll) │         │Consumer            │
└──────┬───────────────┘         └────────┬───────────┘
       │                                   │
       │  WebSocket broadcast              │  MongoDB save
       │  /topic/messages/{roomId}         │  + WebSocket status
       │                                   │  /topic/status/{roomId}
       ▼                                   ▼
┌─────────────────────────────────────────────────────┐
│                 Client (구독자들)                     │
└─────────────────────────────────────────────────────┘
```

### 코드 흐름

#### 1. MessagePublisherAdapter (Dual Publishing)

**파일**: `adapter/out/message/MessagePublisherAdapter.kt`

```kotlin
// Line 60-81
override fun publish(request: ChatMessageRequest, domainMessage: ChatMessage) {
    applicationCoroutineScope.launch {
        try {
            // 경로 1: Redis Stream으로 즉시 브로드캐스트
            publishToRedis(request)  // 1-5ms

            // 경로 2: Kafka로 백그라운드 영속화
            val event = messageDomainService.createMessageEvent(domainMessage)
            publishToKafkaAsync(event)  // 10-50ms

            publishDomainEvents(domainMessage)
        } catch (throwable: Throwable) {
            handlePublishError(request, request.tempId.orEmpty(), throwable)
        }
    }
}

// Line 174-188: Redis Stream publish
private suspend fun publishToRedis(message: ChatMessageRequest) {
    val streamKey = "stream:chat:room:${message.roomId}"
    val messageJson = objectMapper.writeValueAsString(message)
    val record = StreamRecords.newRecord()
        .ofMap(mapOf("message" to messageJson))
        .withStreamKey(streamKey)

    redisTemplate.opsForStream<String, String>().add(record)
}

// Line 197-207: Kafka publish
private suspend fun publishToKafka(topic: String, key: String, event: MessageEvent) {
    kafkaTemplate.send(topic, key, event).await()
}
```

#### 2. MessageRedisStreamListener (Redis Consumer)

**파일**: `adapter/in/redis/MessageRedisStreamListener.kt`

```kotlin
// Line 47-54: 폴링 시작
@PostConstruct
fun init() {
    redisStreamManager.createConsumerGroups(streamKeyPattern, consumerGroup)
    startPolling()  // 100ms 간격 폴링
}

// Line 102-117: 메시지 폴링
private suspend fun pollMessages() {
    val streamKeys = redisStreamManager.scanStreamKeys("stream:chat:room:*")
    streamKeys.chunked(maxConcurrentStreams).forEach { chunk ->
        processStreamKeysInParallel(chunk)
    }
}

// Line 243-253: WebSocket broadcast
private fun processMessage(record: MapRecord<*, *, *>) {
    val roomId = extractRoomId(streamKey)
    val messageValue = extractMessageValue(record)
    redisMessageProcessor.processMessage(roomId, messageValue)
    // → WebSocket /topic/messages/{roomId}
}
```

#### 3. MessageKafkaConsumer (Kafka Consumer)

**파일**: `adapter/in/kafka/MessageKafkaConsumer.kt`

```kotlin
// Line 16-32
@KafkaListener(topics = ["chat-messages"], groupId = "shoot")
fun consumeMessage(@Payload event: MessageEvent, acknowledgment: Acknowledgment) {
    if (event.type == EventType.MESSAGE_CREATED) {
        val success = handleMessageEventUseCase.handle(event)
        // → MongoDB 저장 + WebSocket /topic/status/{roomId}
        if (success) {
            acknowledgment.acknowledge()
        }
    }
}
```

### 특징

- **두 개의 Consumer**: MessageRedisStreamListener + MessageKafkaConsumer
- **두 개의 WebSocket 경로**:
  - `/topic/messages/{roomId}` (Redis Stream, 메시지 전체)
  - `/topic/status/{roomId}` (Kafka, 상태 업데이트)
- **Redis Stream 설정**:
  - Polling interval: 100ms
  - Consumer group: chat-consumers
  - Max concurrent streams: 10

---

## 문제점 분석

### 1. 메시지 중복 가능성

**문제**: 같은 메시지가 두 경로로 전송

```
Client가 받는 메시지:
1. Redis Stream → /topic/messages/{roomId} (SENDING 상태, 1-5ms)
2. Kafka → /topic/status/{roomId} (SAVED 상태, 10-50ms)
```

**결과**: 클라이언트가 `messageId` 기반 중복 제거 로직 필수

### 2. 순서 보장 복잡성

**문제**: Redis Stream과 Kafka의 전송 속도 차이로 순서 역전 가능

```
시나리오:
- Message A (10:00:00.000) → Redis (10:00:00.003) ✅ 먼저 도착
- Message B (10:00:00.001) → Redis (10:00:00.008)

- Message A → Kafka (10:00:00.025)
- Message B → Kafka (10:00:00.020) ✅ 먼저 도착 (역전!)
```

**결과**:
- Redis Stream 경로: A → B (정상)
- Kafka 경로: B → A (역전)
- 클라이언트 UI 혼란 가능

### 3. 장애 시나리오

**문제**: Partial Failure 발생 가능

```kotlin
// MessagePublisherAdapter.kt:60-81
publishToRedis(request)        // ✅ 성공 (사용자는 메시지를 봄)
publishToKafkaAsync(event)     // ❌ 실패 (DB에 저장 안됨)
```

**결과**:
- 사용자는 메시지를 **봤지만** (Redis Stream)
- DB에는 **저장 안됨** (Kafka 실패)
- 앱 재시작 시 메시지 사라짐 → 데이터 불일치

### 4. 운영 복잡성

#### 모니터링 포인트

| 항목 | Redis Stream | Kafka | 합계 |
|------|-------------|-------|------|
| **Broker** | Redis 클러스터 | Kafka 클러스터 | 2개 |
| **Consumer** | MessageRedisStreamListener | MessageKafkaConsumer | 2개 |
| **Consumer Lag** | PEL 모니터링 | Consumer Lag | 2개 |
| **에러 핸들링** | 폴링 재시도 로직 | Kafka 재시도 로직 | 2개 |
| **설정 관리** | application.yml (Redis) | application.yml (Kafka) | 2개 |

#### 운영 이슈 예시

```yaml
# Redis Stream 설정
app.redis-stream.polling-interval-ms: 100
app.redis-stream.error-retry-delay-ms: 1000
app.redis-stream.consumer-group: chat-consumers
app.redis-stream.max-concurrent-streams: 10

# Kafka 설정
spring.kafka.consumer.group-id: shoot
spring.kafka.consumer.auto-offset-reset: earliest
spring.kafka.consumer.enable-auto-commit: false
```

**문제**: 설정 변경 시 두 시스템 모두 고려 필요

### 5. 비용 증가

**10만 메시지/일 기준 예상 비용** (AWS 기준):

| 항목 | Redis Stream | Kafka | Dual (현재) |
|------|--------------|-------|-------------|
| **인프라** | ElastiCache (r6g.large) | MSK (kafka.m5.large) | 둘 다 |
| **월 비용** | ~$150 | ~$200 | ~$350 |
| **처리량** | ~100K msg/s | ~1M msg/s | Redis 병목 |
| **저장 용량** | AOF snapshot | Native (압축) | 중복 저장 |

**결론**: Redis Stream은 처리량 대비 비용 비효율적

### 6. 코드 복잡성

**영향 받는 컴포넌트**:

```
adapter/
├── in/
│   ├── redis/
│   │   ├── MessageRedisStreamListener.kt        (292줄)
│   │   └── util/
│   │       ├── RedisMessageProcessor.kt         (100줄+)
│   │       └── RedisStreamManager.kt            (150줄+)
│   └── kafka/
│       └── MessageKafkaConsumer.kt              (34줄)
└── out/
    └── message/
        └── MessagePublisherAdapter.kt           (260줄, 두 경로 관리)

infrastructure/
└── config/
    ├── RedisConfig.kt                           (Redis Stream 설정)
    └── KafkaConfig.kt                           (Kafka 설정)
```

**총 라인 수**: ~800+ 줄 (Redis Stream 관련)

---

## 의사결정 과정

### 비교 분석: Redis Stream vs Kafka vs Both

| 항목 | Redis Stream Only | Kafka Only | Both (현재) |
|------|-------------------|------------|-------------|
| **레이턴시** | 1-5ms ⭐⭐⭐⭐⭐ | 10-50ms ⭐⭐⭐ | 1-5ms (Redis 경로) ⭐⭐⭐⭐⭐ |
| **처리량** | ~100K msg/s ⭐⭐⭐ | ~1M msg/s ⭐⭐⭐⭐⭐ | ~100K msg/s (병목) ⭐⭐⭐ |
| **영속성** | AOF (느림) ⭐⭐ | Native ⭐⭐⭐⭐⭐ | Kafka에서 처리 ⭐⭐⭐⭐ |
| **순서 보장** | Stream 내 ⭐⭐⭐⭐ | Partition 내 ⭐⭐⭐⭐ | ⚠️ 두 경로 독립적 ⭐⭐ |
| **장애 복구** | Snapshot ⭐⭐ | Offset ⭐⭐⭐⭐⭐ | ⚠️ 불일치 가능 ⭐⭐ |
| **확장성** | 수평 확장 제한적 ⭐⭐ | 파티션 확장 ⭐⭐⭐⭐⭐ | Kafka 확장 ⭐⭐⭐⭐ |
| **운영 복잡도** | 낮음 ⭐⭐ | 중간 ⭐⭐⭐ | 매우 높음 ⭐⭐⭐⭐⭐ |
| **비용** (10만 msg/일) | ~$150/월 ⭐⭐⭐⭐ | ~$200/월 ⭐⭐⭐ | ~$350/월 ⭐⭐ |
| **적합 규모** | ~5만 유저 | 10만+ 유저 | 비효율적 |

### 실제 사례 분석

#### Slack
- **초기**: Redis Pub/Sub (낮은 지연)
- **문제**: 확장성 한계 (메시지 유실, 순서 보장 어려움)
- **전환**: Kafka + WebSocket (단일 경로)
- **결과**: 1000만+ 동시 접속 지원

#### Discord
- **아키텍처**: Kafka-like (Scylla) + WebSocket
- **특징**: 단일 메시지 경로 (중복 없음)
- **성능**: 2.5억+ 유저 지원

#### Telegram
- **아키텍처**: Custom MTProto (P2P + Server)
- **특징**: 단일 경로, 초저지연 (5-10ms)
- **최적화**: 클라이언트 캐싱 + 서버 분산

### 결론: Kafka Only 선택

**이유**:
1. **10만 유저 목표**: Kafka 처리량 충분 (~1M msg/s)
2. **레이턴시 허용 가능**: 20-30ms는 네트워크 latency에 비해 미미
3. **단순성**: 단일 진실 공급원, 운영 복잡도 감소
4. **비용 효율**: Redis Stream 제거로 ~40% 비용 절감
5. **확장성**: 파티션 추가로 선형 확장 가능

---

## 최종 아키텍처 (Kafka Only)

### 구조도

```
┌─────────────────────────────────────────────────────────┐
│                   Client (WebSocket)                     │
└─────────────────────┬───────────────────────────────────┘
                      │ STOMP /app/chat/send
                      ▼
             ┌────────────────────┐
             │ MessageStompHandler │
             └────────┬───────────┘
                      │
                      ▼
          ┌───────────────────────┐
          │ MessagePublisherAdapter│
          └──────────┬────────────┘
                     │
                     │ Kafka produce
                     ▼
              ┌──────────────┐
              │    Kafka     │
              │ (통합 처리)   │
              └──────┬───────┘
                     │
                     ▼
         ┌────────────────────┐
         │ MessageKafkaConsumer│
         └────────┬───────────┘
                  │
                  ▼
         ┌────────────────────┐
         │ HandleMessageEvent  │
         │ UseCase             │
         └────┬───────────┬───┘
              │           │
    ┌─────────┘           └──────────┐
    ▼                                ▼
┌─────────┐                  ┌───────────────┐
│MongoDB  │                  │ WebSocket     │
│(영속화) │                  │ Broadcast     │
└─────────┘                  │ /topic/       │
                             │ messages/     │
                             │ {roomId}      │
                             └───────┬───────┘
                                     ▼
                         ┌───────────────────┐
                         │ Client (구독자들)  │
                         └───────────────────┘
```

### 주요 변경 사항

#### 1. 단일 메시지 경로

**Before (Dual)**:
```kotlin
override fun publish(request: ChatMessageRequest, domainMessage: ChatMessage) {
    publishToRedis(request)        // 경로 1
    publishToKafkaAsync(event)     // 경로 2
}
```

**After (Kafka Only)**:
```kotlin
override fun publish(request: ChatMessageRequest, domainMessage: ChatMessage) {
    val event = messageDomainService.createMessageEvent(domainMessage)
    publishToKafka(event)  // 단일 경로
}
```

#### 2. WebSocket 통합

**Before**:
- `/topic/messages/{roomId}` (Redis Stream, 메시지 전체)
- `/topic/status/{roomId}` (Kafka, 상태 업데이트)

**After**:
- `/topic/messages/{roomId}` (Kafka, 메시지 + 상태)

#### 3. Consumer 단순화

**Before**:
- MessageRedisStreamListener (292줄, 100ms 폴링)
- MessageKafkaConsumer (34줄)

**After**:
- MessageKafkaConsumer만 사용 (최적화됨)

---

## 제거된 컴포넌트

### 삭제할 파일 목록

```
src/main/kotlin/com/stark/shoot/adapter/in/redis/
├── MessageRedisStreamListener.kt                  [DELETE]
└── util/
    ├── RedisMessageProcessor.kt                   [DELETE]
    └── RedisStreamManager.kt                      [DELETE]
```

### 수정할 파일 목록

#### 1. MessagePublisherAdapter.kt

**제거 대상**:
- `publishToRedis()` 메서드 (Line 174-188)
- `redisTemplate` 의존성
- Redis Stream 관련 import

**유지 대상**:
- `publishToKafka()` 메서드 (최적화)
- `publishDomainEvents()` 메서드
- 에러 핸들링 로직

#### 2. application.yml

**제거 대상**:
```yaml
app:
  redis-stream:
    polling-interval-ms: 100
    error-retry-delay-ms: 1000
    consumer-group: chat-consumers
    stream-key-pattern: stream:chat:room:*
    max-concurrent-streams: 10
```

#### 3. build.gradle.kts

**확인 사항**:
- `spring-boot-starter-data-redis` 의존성은 **유지** (캐싱, 세션용)
- Redis Stream 전용 의존성이 있다면 제거

---

## 성능 최적화

### Kafka Producer 최적화

**목표**: 레이턴시 10-50ms → 15-30ms로 개선

#### 1. Producer 설정

**파일**: `infrastructure/config/KafkaConfig.kt`

```kotlin
@Bean
fun kafkaProducerConfig(): Map<String, Any> = mapOf(
    // 기본 설정
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,

    // 레이턴시 최적화
    ProducerConfig.LINGER_MS_CONFIG to 0,           // 즉시 전송 (배칭 안함)
    ProducerConfig.COMPRESSION_TYPE_CONFIG to "none", // 압축 안함 (CPU 절약)
    ProducerConfig.ACKS_CONFIG to "1",              // leader만 확인 (빠름)
    ProducerConfig.BATCH_SIZE_CONFIG to 16384,      // 작은 배치 크기
    ProducerConfig.BUFFER_MEMORY_CONFIG to 33554432, // 32MB 버퍼

    // 재시도 설정 (신뢰성)
    ProducerConfig.RETRIES_CONFIG to 3,
    ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to 5,
    ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true // 중복 방지
)
```

**설정 설명**:

| 설정 | 값 | 이유 |
|------|-----|------|
| `linger.ms` | 0 | 즉시 전송 (배칭 대기 안함) |
| `compression.type` | none | CPU 오버헤드 제거 |
| `acks` | 1 | leader만 확인 (all=느림) |
| `batch.size` | 16KB | 작은 배치로 빠른 전송 |
| `enable.idempotence` | true | 중복 메시지 방지 |

### Kafka Consumer 최적화

#### 1. Consumer 설정

```kotlin
@Bean
fun kafkaConsumerConfig(): Map<String, Any> = mapOf(
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
    ConsumerConfig.GROUP_ID_CONFIG to "shoot",
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,

    // 레이턴시 최적화
    ConsumerConfig.FETCH_MIN_BYTES_CONFIG to 1,        // 최소 1바이트만 있어도 즉시 반환
    ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to 10,     // 최대 10ms 대기
    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 100,     // 한번에 100개 처리

    // 신뢰성
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false, // 수동 ACK
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest"
)
```

#### 2. MessageKafkaConsumer 개선

**변경 사항**:

```kotlin
@Component
class MessageKafkaConsumer(
    private val handleMessageEventUseCase: HandleMessageEventUseCase,
    private val webSocketMessageBroker: WebSocketMessageBroker
) {

    @KafkaListener(
        topics = ["chat-messages"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory",
        concurrency = "3"  // 병렬 처리 (파티션 3개 가정)
    )
    fun consumeMessage(
        @Payload event: MessageEvent,
        acknowledgment: Acknowledgment
    ) {
        if (event.type == EventType.MESSAGE_CREATED) {
            try {
                // 1. MongoDB 저장 (async)
                val saved = handleMessageEventUseCase.handle(event)

                if (saved) {
                    // 2. WebSocket 즉시 전송 (실시간성)
                    broadcastMessage(event)

                    // 3. ACK 처리
                    acknowledgment.acknowledge()
                }
            } catch (e: Exception) {
                logger.error(e) { "메시지 처리 실패: ${event.data.id}" }
                // 실패 시 ACK 안함 → Kafka 재시도
            }
        }
    }

    private fun broadcastMessage(event: MessageEvent) {
        val roomId = event.data.roomId.value
        webSocketMessageBroker.sendMessage(
            "/topic/messages/$roomId",
            event.data
        )
    }
}
```

### Kafka 토픽 최적화

```bash
# 채팅방별 순서 보장을 위한 파티션 설정
kafka-topics.sh --create \
  --topic chat-messages \
  --partitions 3 \          # 병렬 처리 (consumer concurrency=3과 일치)
  --replication-factor 2 \  # 신뢰성 (최소 2개 복제본)
  --config min.insync.replicas=1 \     # leader만 확인 (빠름)
  --config retention.ms=604800000 \    # 7일 보관
  --config compression.type=producer   # producer 설정 따름
```

### 예상 성능

#### 레이턴시 분석

| 단계 | 시간 | 누적 |
|------|------|------|
| **Producer**: Kafka send | 5-10ms | 5-10ms |
| **Broker**: Leader write | 2-5ms | 7-15ms |
| **Consumer**: Fetch | 1-3ms | 8-18ms |
| **MongoDB**: Save (async) | 5-10ms | 병렬 처리 |
| **WebSocket**: Broadcast | 2-5ms | 10-23ms |
| **총 레이턴시** | | **10-23ms** |

**네트워크 latency 비교**:
- Seoul → Tokyo: ~30-50ms
- Seoul → US-West: ~100-150ms
- WiFi latency: ~10-30ms

**결론**: Kafka 레이턴시 (10-23ms)는 네트워크 latency에 비해 미미하여 체감 불가

#### 처리량

- **Kafka 처리량**: ~1M msg/s (파티션 3개 기준)
- **10만 유저 목표**:
  - 동시 접속 10%: 1만명
  - 평균 1msg/분: ~167 msg/s
  - 피크 시간 (10배): ~1,670 msg/s
- **여유율**: 약 **600배**

---

## 마이그레이션 체크리스트

### Phase 1: 코드 제거

- [ ] `MessageRedisStreamListener.kt` 삭제
- [ ] `RedisMessageProcessor.kt` 삭제
- [ ] `RedisStreamManager.kt` 삭제
- [ ] `MessagePublisherAdapter.publishToRedis()` 제거
- [ ] `MessagePublisherAdapter` Redis 의존성 제거

### Phase 2: Kafka 최적화

- [ ] `KafkaConfig.kt` producer 설정 최적화
- [ ] `KafkaConfig.kt` consumer 설정 최적화
- [ ] `MessageKafkaConsumer` WebSocket broadcast 추가
- [ ] Consumer concurrency 설정 (3개)

### Phase 3: 설정 정리

- [ ] `application.yml` Redis Stream 설정 제거
- [ ] Kafka 파티션 3개로 재생성 (기존 데이터 백업)
- [ ] 모니터링 대시보드 업데이트 (Redis Stream 메트릭 제거)

### Phase 4: 테스트

- [ ] 단위 테스트: MessageKafkaConsumer
- [ ] 통합 테스트: Kafka end-to-end flow
- [ ] 성능 테스트: 레이턴시 측정 (목표: 30ms 이하)
- [ ] 부하 테스트: 5,000 msg/s 처리 검증

### Phase 5: 배포

- [ ] Staging 환경 배포 및 검증
- [ ] Production 배포 (Blue-Green)
- [ ] 롤백 계획 준비 (Redis Stream 코드 백업)
- [ ] 모니터링 알람 설정

---

## 결론

### 개선 효과

| 항목 | Before (Dual) | After (Kafka Only) | 개선율 |
|------|---------------|-------------------|--------|
| **레이턴시** | 1-5ms (Redis) | 10-23ms (Kafka) | +15ms (허용) |
| **운영 복잡도** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | **40% 감소** |
| **월 비용** | ~$350 | ~$200 | **43% 절감** |
| **코드 라인 수** | ~800+ | ~200 | **75% 감소** |
| **장애 포인트** | 2개 (Redis+Kafka) | 1개 (Kafka) | **50% 감소** |
| **확장성** | Redis 병목 | Kafka 선형 확장 | **무제한** |

### 핵심 메시지

1. **단순함이 최고**: 두 시스템보다 하나가 낫다
2. **확장성 우선**: 10만 유저 목표 달성 가능
3. **비용 효율**: 40% 비용 절감
4. **운영 효율**: 단일 진실 공급원으로 일관성 확보
5. **성능 허용**: 15ms 레이턴시 증가는 체감 불가

---

**Last updated**: 2025-10-24
**Author**: Architecture Team
**Status**: ✅ Approved for implementation
