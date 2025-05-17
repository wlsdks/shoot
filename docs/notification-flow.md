# 알림 생성 및 전달 프로세스 가이드

## 개요
이 문서는 알림이 처음 생성되어 저장되고 사용자에게 전달되는 전체 프로세스를 설명합니다. 특히 채팅 메시지에서 알림이 어떻게 생성되고 처리되는지 상세히 다룹니다.

## 알림 시스템 아키텍처

알림 시스템은 헥사고날 아키텍처를 기반으로 구현되어 있으며, 다음과 같은 주요 컴포넌트로 구성됩니다:

1. **도메인 모델**: 알림의 핵심 비즈니스 로직과 데이터 구조
2. **포트**: 애플리케이션과 외부 시스템 간의 인터페이스
3. **어댑터**: 포트의 구현체로, 실제 외부 시스템과 통신

### 도메인 모델

알림 시스템의 핵심 도메인 모델은 다음과 같습니다:

- **Notification**: 알림의 기본 데이터 구조
  - `id`: 알림 고유 식별자
  - `userId`: 알림을 받을 사용자 ID
  - `title`: 알림 제목
  - `message`: 알림 내용
  - `type`: 알림 유형 (NotificationType)
  - `sourceId`: 알림 소스 ID (예: 채팅방 ID)
  - `sourceType`: 알림 소스 유형 (SourceType)
  - `isRead`: 읽음 여부
  - `createdAt`: 생성 시간
  - `readAt`: 읽은 시간
  - `metadata`: 추가 정보

- **NotificationType**: 알림 유형을 정의하는 열거형
  - `NEW_MESSAGE`: 새 메시지 알림
  - `MENTION`: 멘션 알림
  - `REACTION`: 반응 알림
  - `PIN`: 고정 메시지 알림
  - 기타 친구 관련 및 시스템 알림 유형

- **SourceType**: 알림 소스 유형을 정의하는 열거형
  - `CHAT`: 채팅 메시지
  - `CHAT_ROOM`: 채팅방
  - `USER`: 사용자
  - `FRIEND`: 친구
  - `SYSTEM`: 시스템
  - `OTHER`: 기타

## 알림 생성 프로세스

### 1. 채팅 이벤트 발생

알림 생성 프로세스는 채팅 이벤트가 발생할 때 시작됩니다. 채팅 이벤트는 다음과 같은 상황에서 발생할 수 있습니다:

- 새로운 채팅 메시지 생성
- 사용자가 메시지에서 멘션됨
- 메시지에 반응이 추가됨
- 메시지가 고정됨

### 2. 이벤트 리스너가 이벤트 수신

`ChatEventNotificationListener` 클래스는 Spring의 이벤트 리스너 메커니즘을 사용하여 채팅 이벤트를 수신합니다:

```kotlin
@EventListener
fun handleChatEvent(event: ChatEvent) {
    handleMessageCreated(event)
}
```

### 3. 알림 수신자 식별

이벤트를 수신한 후, 리스너는 알림을 받아야 할 사용자를 식별합니다:

```kotlin
private fun identifyRecipients(message: ChatMessage): Set<Long> {
    return message.readBy.keys.filter { it != message.senderId }.toSet()
}
```

이 과정에서 메시지 발신자는 자신의 메시지에 대한 알림을 받지 않도록 제외됩니다.

### 4. 알림 생성

식별된 수신자에 대해 `ChatNotificationFactory`를 사용하여 알림을 생성합니다:

#### 4.1 멘션 알림 생성

메시지에 멘션된 사용자가 있는 경우, 해당 사용자에 대한 멘션 알림을 생성합니다:

```kotlin
private fun createMentionNotifications(message: ChatMessage): List<Notification> {
    // 멘션된 사용자가 없으면 빈 리스트 반환
    if (message.mentions.isEmpty()) {
        return emptyList()
    }

    // 자신을 멘션한 경우는 제외
    val mentionedUsers = message.mentions.filter { it != message.senderId }.toSet()
    if (mentionedUsers.isEmpty()) {
        return emptyList()
    }

    // 멘션된 사용자에 대한 알림 생성
    return mentionedUsers.map { userId ->
        chatNotificationFactory.createMentionNotification(
            userId = userId,
            message = message
        )
    }
}
```

`ChatNotificationFactory`의 `createMentionNotification` 메서드는 다음과 같이 멘션 알림을 생성합니다:

```kotlin
fun createMentionNotification(userId: Long, message: ChatMessage): Notification {
    val truncatedText = truncateMessageText(message.content.text)

    return Notification.fromChatEvent(
        userId = userId,
        title = "새로운 멘션",
        message = "메시지에서 언급되었습니다: $truncatedText",
        type = NotificationType.MENTION,
        sourceId = message.roomId.toString(),
        metadata = createMessageMetadata(message)
    )
}
```

#### 4.2 일반 메시지 알림 생성

멘션 알림 외에도, 채팅방의 모든 참여자(발신자 제외)에게 일반 메시지 알림을 생성합니다:

```kotlin
private fun createMessageNotifications(
    message: ChatMessage,
    recipients: Set<Long>
): List<Notification> {
    return recipients.map { userId ->
        chatNotificationFactory.createMessageNotification(
            userId = userId,
            message = message
        )
    }
}
```

`ChatNotificationFactory`의 `createMessageNotification` 메서드는 다음과 같이 일반 메시지 알림을 생성합니다:

```kotlin
fun createMessageNotification(userId: Long, message: ChatMessage): Notification {
    val truncatedText = truncateMessageText(message.content.text)

    return Notification.fromChatEvent(
        userId = userId,
        title = "새로운 메시지",
        message = truncatedText,
        type = NotificationType.NEW_MESSAGE,
        sourceId = message.roomId.toString(),
        metadata = createMessageMetadata(message)
    )
}
```

### 5. 알림 저장

생성된 알림은 `SaveNotificationPort`를 통해 MongoDB에 저장됩니다:

```kotlin
// DB에 알림 저장
val savedNotifications = saveNotificationPort.saveNotifications(notifications)
```

`SaveNotificationPort`의 구현체인 `SaveNotificationMongoAdapter`는 알림을 MongoDB에 저장합니다:

```kotlin
override fun saveNotifications(notifications: List<Notification>): List<Notification> {
    val documents = notifications.map { NotificationDocument.fromDomain(it) }
    val savedDocuments = notificationMongoRepository.saveAll(documents)
    return savedDocuments.map { it.toDomain() }
}
```

이 과정에서 도메인 객체인 `Notification`은 MongoDB 문서인 `NotificationDocument`로 변환되어 저장됩니다.

### 6. 알림 전송

저장된 알림은 `SendNotificationPort`를 통해 실시간으로 사용자에게 전송됩니다:

```kotlin
// 실시간 알림 전송
sendNotificationPort.sendNotifications(savedNotifications)
```

#### 6.1 Redis를 통한 알림 전송

`SendNotificationPort`의 구현체인 `SendNotificationRedisAdapter`는 Redis Pub/Sub을 사용하여 알림을 전송합니다:

```kotlin
override fun sendNotifications(notifications: List<Notification>) {
    if (notifications.isEmpty()) {
        logger.info { "전송할 알림이 없습니다." }
        return
    }

    try {
        // 사용자별로 알림 그룹화
        val notificationsByUser = notifications.groupBy { it.userId }

        // 각 사용자별로 알림 전송
        notificationsByUser.forEach { (userId, userNotifications) ->
            val channel = "$NOTIFICATION_CHANNEL_PREFIX$userId"

            // 각 알림을 개별적으로 발행
            userNotifications.forEach { notification ->
                val notificationJson = objectMapper.writeValueAsString(notification)
                redisTemplate.convertAndSend(channel, notificationJson)
            }

            logger.info { "사용자($userId)에게 ${userNotifications.size}개의 알림이 Redis 채널에 발행되었습니다." }
        }
    } catch (e: Exception) {
        val errorMessage = "Redis를 통한 다중 알림 전송 중 오류가 발생했습니다: ${e.message}"
        logger.error(e) { errorMessage }
        throw RedisOperationException(errorMessage, e)
    }
}
```

이 과정에서 알림은 사용자별로 그룹화되어 각 사용자의 Redis 채널에 발행됩니다. 채널 이름은 `notification:user:{userId}` 형식으로 구성됩니다.

#### 6.2 Kafka를 통한 알림 전송

`SendNotificationPort`의 또 다른 구현체인 `SendNotificationKafkaAdapter`는 Kafka를 사용하여 알림을 전송합니다:

```kotlin
override fun sendNotification(notification: Notification) {
    try {
        // 사용자 ID를 파티션 키로 사용하여 같은 사용자의 알림이 순서대로 처리되도록 함
        val key = notification.userId.toString()
        val notificationJson = objectMapper.writeValueAsString(notification)

        // Kafka 토픽에 알림 발행
        val future = kafkaTemplate.send(NOTIFICATION_TOPIC, key, notificationJson)

        future.whenComplete { result, ex ->
            if (ex != null) {
                logger.error(ex) { "Kafka를 통한 알림 전송 중 오류가 발생했습니다: ${ex.message}" }
                throw KafkaPublishException("Kafka를 통한 알림 전송 중 오류가 발생했습니다: ${ex.message}", ex)
            } else {
                logger.info { "알림이 Kafka 토픽에 발행되었습니다: userId=${notification.userId}, type=${notification.type}, offset=${result.recordMetadata.offset()}" }
            }
        }
    } catch (e: Exception) {
        val errorMessage = "Kafka를 통한 알림 전송 중 오류가 발생했습니다: ${e.message}"
        logger.error(e) { errorMessage }
        throw RedisOperationException(errorMessage, e)
    }
}
```

Kafka를 사용한 알림 전송은 다음과 같은 특징이 있습니다:

1. **내구성**: Kafka는 메시지를 디스크에 저장하므로, 서버 재시작 후에도 알림이 손실되지 않습니다.
2. **순서 보장**: 사용자 ID를 파티션 키로 사용하여 같은 사용자의 알림이 순서대로 처리됩니다.
3. **확장성**: Kafka는 높은 처리량을 지원하므로, 대량의 알림을 처리할 수 있습니다.
4. **통합**: 다른 시스템이 Kafka 토픽을 구독하여 알림을 처리할 수 있습니다.

#### 6.3 알림 전송 방식 선택

애플리케이션은 설정에 따라 Redis 또는 Kafka를 사용하여 알림을 전송할 수 있습니다. 이는 `NotificationConfig` 클래스에서 설정됩니다:

```yaml
# application.yml
notification:
  transport: redis  # 또는 kafka
```

Redis는 실시간성이 중요한 경우에 적합하고, Kafka는 내구성과 확장성이 중요한 경우에 적합합니다.

## 알림 수신 프로세스

클라이언트는 다음과 같은 방법으로 알림을 수신할 수 있습니다:

1. **웹소켓 연결**: 클라이언트는 웹소켓을 통해 서버에 연결하고, 서버는 Redis 채널에서 수신한 알림을 웹소켓을 통해 클라이언트에 전달합니다.

2. **Server-Sent Events(SSE)**: 클라이언트는 SSE 연결을 통해 서버로부터 알림을 스트리밍 방식으로 수신합니다.

3. **폴링**: 클라이언트는 주기적으로 서버에 요청하여 새로운 알림이 있는지 확인합니다.

## 알림 관리 API

사용자는 다음과 같은 API를 통해 알림을 관리할 수 있습니다:

1. **알림 조회**: 사용자의 모든 알림 또는 특정 조건에 맞는 알림을 조회합니다.
2. **알림 읽음 처리**: 특정 알림 또는 모든 알림을 읽음 상태로 변경합니다.
3. **알림 삭제**: 특정 알림 또는 모든 알림을 삭제합니다.

## 요약: 알림 생성부터 전달까지의 전체 흐름

1. **이벤트 발생**: 채팅 메시지 생성과 같은 이벤트가 발생합니다.
2. **이벤트 발행**: 이벤트가 Spring의 이벤트 시스템을 통해 발행됩니다.
3. **이벤트 수신**: `ChatEventNotificationListener`가 이벤트를 수신합니다.
4. **수신자 식별**: 알림을 받아야 할 사용자가 식별됩니다.
5. **알림 생성**: `ChatNotificationFactory`를 사용하여 알림이 생성됩니다.
6. **알림 저장**: 생성된 알림이 MongoDB에 저장됩니다.
7. **알림 전송**: 저장된 알림이 Redis Pub/Sub 또는 Kafka를 통해 사용자에게 전송됩니다(설정에 따라 다름).
8. **알림 수신**: 클라이언트가 웹소켓, SSE 등을 통해 알림을 수신합니다.
9. **알림 처리**: 사용자가 알림을 읽거나 삭제합니다.

이 문서는 알림 시스템의 전체 흐름을 설명하며, 특히 채팅 메시지에서 알림이 어떻게 생성되고 처리되는지에 중점을 둡니다. 알림 시스템의 구현 세부 사항은 [알림 시스템 구현 문서](notification-implementation.md)를 참조하세요.
