# 메시지 반응 알림 구현 문서

## 개요
이 문서는 메시지 반응(리액션)에 대한 알림 기능의 구현 내용을 설명합니다. 사용자가 메시지에 반응을 추가하거나 제거할 때 도메인 이벤트가 발행되고, 이 이벤트를 수신하여 알림을 생성하고 전송하는 과정을 다룹니다.

## 구현된 기능

### 1. 메시지 반응 이벤트 발행
- `ToggleMessageReactionService`에서 반응 추가/제거 시 `MessageReactionEvent`를 발행하도록 수정
- 반응 추가, 제거, 변경 모두에 대해 이벤트 발행
- 리액션 교체 시 이벤트에 교체 여부 표시

### 2. 메시지 반응 이벤트 리스너 구현
- `ReactionEventNotificationListener` 클래스 구현
- 메시지 반응 이벤트를 수신하여 알림 생성 및 전송
- 리액션 교체 시 최종 행위(추가)에 대해서만 알림 생성

### 3. 알림 생성 및 전송 로직
- 메시지 작성자에게만 알림 전송 (자신의 메시지에 대한 반응은 알림 제외)
- 반응 추가 시에만 알림 생성 (반응 제거 시에는 알림 생성하지 않음)
- 알림 저장 및 전송 기능 구현

## 구현 상세 내용

### 1. ToggleMessageReactionService 수정

`ToggleMessageReactionService` 클래스에 `EventPublisher`를 주입하고, 반응 추가/제거 시 이벤트를 발행하도록 수정했습니다:

```kotlin
@UseCase
class ToggleMessageReactionService(
    private val loadMessagePort: LoadMessagePort,
    private val saveMessagePort: SaveMessagePort,
    private val messagingTemplate: SimpMessagingTemplate,
    private val eventPublisher: EventPublisher  // 추가된 의존성
) : ToggleMessageReactionUseCase {
    // ...
}
```

반응 이벤트를 발행하는 헬퍼 메서드를 추가했습니다:

```kotlin
private fun publishReactionEvent(
    messageId: String,
    roomId: Long,
    userId: Long,
    reactionType: String,
    isAdded: Boolean,
    isReplacement: Boolean = false
) {
    val event = MessageReactionEvent(
        messageId = messageId,
        roomId = roomId.toString(),
        userId = userId.toString(),
        reactionType = reactionType,
        isAdded = isAdded,
        isReplacement = isReplacement
    )

    eventPublisher.publish(event)
}
```

다음 세 가지 상황에서 이벤트를 발행하도록 수정했습니다:

1. 같은 리액션을 선택하여 제거하는 경우:
```kotlin
// 도메인 이벤트 발행 (리액션 제거)
publishReactionEvent(messageId, message.roomId, userId, type.code, false)
```

2. 다른 리액션으로 변경하는 경우 (기존 리액션 제거 후 새 리액션 추가):
```kotlin
// 도메인 이벤트 발행 (리액션 교체)
// 교체 작업에 대해 하나의 이벤트만 발행 (최종 상태인 추가 이벤트)
publishReactionEvent(messageId, message.roomId, userId, newType.code, true, true)
```

여기서 마지막 매개변수 `true`는 이 이벤트가 리액션 교체 작업임을 나타냅니다. 리액션 교체 시 불필요한 이벤트 발행을 줄이기 위해 최종 상태(새 리액션 추가)에 대한 이벤트만 발행합니다. 이를 통해 리스너는 교체 작업에 대해 하나의 알림만 생성할 수 있습니다.

3. 새 리액션을 추가하는 경우:
```kotlin
// 도메인 이벤트 발행 (리액션 추가)
publishReactionEvent(messageId, message.roomId, userId, type.code, true)
```

### 2. MessageReactionEvent 클래스 수정

메시지 반응 이벤트를 나타내는 `MessageReactionEvent` 클래스에 리액션 교체 여부를 나타내는 필드를 추가했습니다:

```kotlin
data class MessageReactionEvent(
    val messageId: String,
    val roomId: String,
    val userId: String,
    val reactionType: String,
    val isAdded: Boolean,  // true: 추가, false: 제거
    val isReplacement: Boolean = false  // true: 리액션 교체의 일부, false: 일반 추가/제거
) : DomainEvent
```

이 필드를 통해 리액션 교체 작업의 일부인 이벤트를 식별할 수 있습니다.

### 3. ReactionEventNotificationListener 구현

메시지 반응 이벤트를 수신하여 알림을 생성하고 전송하는 리스너 클래스를 구현했습니다:

```kotlin
@Component
class ReactionEventNotificationListener(
    private val loadMessagePort: LoadMessagePort,
    private val saveNotificationPort: SaveNotificationPort,
    private val sendNotificationPort: SendNotificationPort
) {
    // ...
}
```

이벤트 리스너 메서드를 구현하여 `MessageReactionEvent`를 수신하도록 했습니다:

```kotlin
@EventListener
fun handleReactionEvent(event: MessageReactionEvent) {
    try {
        // 메시지 조회
        val message = loadMessagePort.findById(event.messageId.toObjectId())
            ?: run {
                logger.warn { "메시지를 찾을 수 없습니다: messageId=${event.messageId}" }
                return
            }

        // 메시지 작성자가 반응을 추가한 사용자와 같으면 알림을 보내지 않음
        val reactingUserId = event.userId.toLong()
        if (message.senderId == reactingUserId) {
            logger.info { "자신의 메시지에 대한 반응은 알림을 생성하지 않습니다: messageId=${event.messageId}" }
            return
        }

        // 반응이 제거된 경우 알림을 생성하지 않음
        if (!event.isAdded) {
            if (event.isReplacement) {
                logger.info { "리액션 교체 중 제거 이벤트이므로 알림을 생성하지 않습니다: messageId=${event.messageId}" }
            } else {
                logger.info { "반응 제거는 알림을 생성하지 않습니다: messageId=${event.messageId}" }
            }
            return
        }

        // 리액션 교체의 일부인 경우, 추가 이벤트에 대해서만 알림을 생성함
        if (event.isReplacement && event.isAdded) {
            logger.info { "리액션 교체 중 추가 이벤트에 대해 알림을 생성합니다: messageId=${event.messageId}" }
        }

        // 알림 생성
        val notification = createReactionNotification(
            userId = message.senderId,
            reactingUserId = reactingUserId,
            messageId = event.messageId,
            roomId = event.roomId,
            reactionType = event.reactionType
        )

        // 알림 저장 및 전송
        val savedNotification = saveNotificationPort.saveNotification(notification)
        sendNotificationPort.sendNotification(savedNotification)

        logger.info { "메시지 반응 알림이 생성되고 전송되었습니다: messageId=${event.messageId}, userId=${message.senderId}" }

    } catch (e: Exception) {
        logger.error(e) { "메시지 반응 이벤트 처리 중 오류가 발생했습니다: ${e.message}" }
    }
}
```

반응 알림을 생성하는 메서드를 구현했습니다:

```kotlin
private fun createReactionNotification(
    userId: Long,
    reactingUserId: Long,
    messageId: String,
    roomId: String,
    reactionType: String
): Notification {
    return Notification.fromChatEvent(
        userId = userId,
        title = "새로운 반응",
        message = "내 메시지에 새로운 반응이 추가되었습니다",
        type = NotificationType.REACTION,
        sourceId = roomId,
        metadata = mapOf(
            "messageId" to messageId,
            "reactingUserId" to reactingUserId.toString(),
            "reactionType" to reactionType
        )
    )
}
```

## 알림 처리 흐름

1. 사용자가 메시지에 반응을 추가하거나 제거합니다.
2. `ToggleMessageReactionService`가 반응을 처리하고 `MessageReactionEvent`를 발행합니다.
3. `ReactionEventNotificationListener`가 이벤트를 수신합니다.
4. 리스너는 메시지를 조회하고, 알림 생성 조건을 확인합니다:
   - 자신의 메시지에 대한 반응인 경우 알림을 생성하지 않습니다.
   - 반응이 제거된 경우 알림을 생성하지 않습니다.
   - 리액션 교체 작업의 일부인 경우:
     - 제거 이벤트(isReplacement=true, isAdded=false)에 대해서는 알림을 생성하지 않습니다.
     - 추가 이벤트(isReplacement=true, isAdded=true)에 대해서는 알림을 생성합니다.
5. 조건을 만족하면 메시지 작성자에게 알림을 생성합니다.
6. 생성된 알림을 저장하고 전송합니다.

## 결론

이 구현을 통해 메시지에 반응이 추가될 때 메시지 작성자에게 알림이 전송됩니다. 특히, 사용자가 기존 리액션을 다른 리액션으로 변경할 때는 최종 행위(새 리액션 추가)에 대해서만 알림이 전송되므로, 불필요한 알림이 발생하지 않습니다.

이러한 알림 시스템은 사용자 경험을 향상시키고, 메시지에 대한 반응을 실시간으로 확인할 수 있게 합니다. 또한, 리액션 교체 시 알림이 중복되지 않도록 하여 사용자에게 더 깔끔한 알림 경험을 제공합니다.
