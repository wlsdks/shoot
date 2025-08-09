# 알림 시스템 구현 문서

## 개요
이 문서는 알림 시스템의 구현 방법과 사용법에 대해 설명합니다. 알림 시스템은 헥사고날 아키텍처 패턴을 따르며, 포트와 어댑터 패턴을 사용하여 구현되었습니다.

## 구현된 컴포넌트

### 1. 포트 (Ports)
- **SendNotificationPort**: 알림을 전송하기 위한 출력 포트 인터페이스
  - `sendNotification(notification: Notification)`: 단일 알림 전송
  - `sendNotifications(notifications: List<Notification>)`: 여러 알림 전송

### 2. 어댑터 (Adapters)
- **SendNotificationRedisAdapter**: Redis를 사용하여 알림을 전송하는 어댑터
  - Redis Pub/Sub 채널을 통해 실시간 알림 전송
  - 사용자별 채널에 알림 발행

- **SendNotificationKafkaAdapter**: Kafka를 사용하여 알림을 전송하는 어댑터
  - Kafka 토픽에 알림 발행
  - 사용자 ID를 파티션 키로 사용하여 순서 보장

- **SendNotificationWebSocketAdapter**: WebSocket을 사용하여 알림을 전송하는 어댑터
  - 사용자별 목적지(`/topic/notifications/{userId}`)로 실시간 알림 전송

### 3. 설정 (Configuration)
- **NotificationConfig**: 알림 전송 방식을 선택하는 설정 클래스
  - `notification.transport` 속성을 통해 Redis, Kafka, WebSocket 중 선택 가능
  - 기본값은 Redis

## 사용 방법

### 1. 설정 방법
`application.yml` 파일에서 알림 전송 방식을 설정할 수 있습니다:

```yaml
# 알림 설정
notification:
  # 알림 전송 방식 (redis, kafka, websocket)
  transport: redis  # 또는 kafka, websocket
```

### 2. 코드에서 사용 방법
알림을 전송하려면 `SendNotificationPort`를 주입받아 사용합니다:

```kotlin
@Service
class NotificationService(
    private val sendNotificationPort: SendNotificationPort
) {
    fun sendNotification(userId: Long, message: String) {
        val notification = Notification(
            userId = userId,
            title = "알림",
            message = message,
            type = NotificationType.SYSTEM_ANNOUNCEMENT,
            sourceId = "system",
            sourceType = SourceType.SYSTEM
        )
        
        sendNotificationPort.sendNotification(notification)
    }
}
```

### 3. 특정 구현체 직접 사용
특정 구현체를 직접 사용하려면 빈 이름을 지정하여 주입받을 수 있습니다:

```kotlin
@Service
class SpecificNotificationService(
    @Qualifier("redisNotificationSender") private val redisNotificationSender: SendNotificationPort,
    @Qualifier("kafkaNotificationSender") private val kafkaNotificationSender: SendNotificationPort,
    @Qualifier("websocketNotificationSender") private val websocketNotificationSender: SendNotificationPort
) {
    fun sendImportantNotification(userId: Long, message: String) {
        val notification = createNotification(userId, message)

        // Redis로 전송
        redisNotificationSender.sendNotification(notification)

        // 중요한 알림은 Kafka로도 전송 (백업 또는 다른 시스템과 통합)
        kafkaNotificationSender.sendNotification(notification)

        // 실시간 WebSocket 알림 전송
        websocketNotificationSender.sendNotification(notification)
    }
    
    private fun createNotification(userId: Long, message: String): Notification {
        return Notification(
            userId = userId,
            title = "중요 알림",
            message = message,
            type = NotificationType.SYSTEM_ANNOUNCEMENT,
            sourceId = "system",
            sourceType = SourceType.SYSTEM
        )
    }
}
```

## 예외 처리
알림 전송 중 오류가 발생하면 다음과 같은 예외가 발생합니다:

- **RedisOperationException**: Redis 작업 중 오류 발생 시
- **KafkaPublishException**: Kafka 발행 중 오류 발생 시 (SendNotificationKafkaAdapter 내부에서 사용)

모든 예외는 적절히 로깅되며, 상위 계층에서 처리할 수 있습니다.

## 클라이언트 구현
### Redis 클라이언트
Redis Pub/Sub을 사용하는 경우, 클라이언트는 다음과 같이 구현할 수 있습니다:

1. 웹소켓을 통해 클라이언트에 알림 전달
2. Server-Sent Events(SSE)를 통해 클라이언트에 알림 전달
3. Redis 구독자가 알림을 수신하여 다른 시스템으로 전달

### Kafka 클라이언트
Kafka를 사용하는 경우, 클라이언트는 다음과 같이 구현할 수 있습니다:

1. Kafka 컨슈머가 알림을 수신하여 처리
2. 다른 시스템이 Kafka 토픽을 구독하여 알림 처리

### WebSocket 클라이언트
WebSocket 방식을 사용하는 경우, 클라이언트는 STOMP 프로토콜을 통해
`/topic/notifications/{userId}` 경로를 구독해야 합니다. 예를 들어
JavaScript에서는 다음과 같이 구독할 수 있습니다:

```javascript
const client = Stomp.over(ws);
client.connect({}, () => {
  client.subscribe('/topic/notifications/123', message => {
    console.log(message.body);
  });
});
```

## 확장 방향
1. 알림 전송 실패 시 재시도 메커니즘 추가
2. 알림 우선순위 지원
3. 알림 그룹화 및 배치 처리
4. 알림 템플릿 시스템 구현