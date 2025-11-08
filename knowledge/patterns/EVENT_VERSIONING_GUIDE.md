# Event Versioning Pattern Guide

## 📋 목차

1. [개요](#개요)
2. [왜 Event Versioning이 필요한가?](#왜-event-versioning이-필요한가)
3. [EventVersion Value Object](#eventversion-value-object)
4. [Event Listener에서 버전 체크](#event-listener에서-버전-체크)
5. [Event Schema 진화 전략](#event-schema-진화-전략)
6. [Breaking Changes 처리](#breaking-changes-처리)
7. [Best Practices](#best-practices)
8. [FAQ](#faq)

---

## 개요

Event Versioning은 MSA 환경에서 **이벤트 스키마의 안전한 진화**를 관리하기 위한 패턴입니다.
서비스 간 비동기 통신에서 발생할 수 있는 버전 불일치 문제를 예방하고, **하위 호환성**을 유지하면서 스키마를 발전시킬 수 있습니다.

### 핵심 구성 요소

1. **EventVersion Value Object** - Semantic Versioning 기반 버전 관리
2. **DomainEvent.version** - 모든 도메인 이벤트에 버전 필드 추가
3. **EventVersionValidator** - Consumer에서 버전 호환성 검증
4. **Event Listeners** - 이벤트 수신 시 버전 체크 로직

---

## 왜 Event Versioning이 필요한가?

### MSA 환경에서의 문제점

```
[User Service]              [Notification Service]
    v2.0                           v1.5
     |                              |
     |-- UserCreatedEvent --------->|
     |   (v2.0 schema)              |
     |                              X  호환되지 않는 필드!
```

**문제 시나리오:**
- User Service가 `UserCreatedEvent`에 새 필드 `phoneNumber` 추가
- Notification Service는 아직 업데이트되지 않음
- 이벤트 파싱 실패 또는 데이터 손실 발생

### Event Versioning으로 해결

```kotlin
// Event Consumer (Notification Service)
@EventListener
fun handleUserCreated(event: UserCreatedEvent) {
    // 버전 체크: Major 버전이 같으면 호환 가능
    EventVersionValidator.checkAndLog(
        event = event,
        expectedVersion = EventVersion.USER_CREATED_V1,
        consumerName = "UserCreatedEventListener"
    )

    // Major 1.x.x 버전들은 모두 호환됨
    // 1.0.0, 1.1.0, 1.2.0 → 호환 O
    // 2.0.0 → 호환 X (경고 로그)
}
```

**장점:**
- ✅ 호환되지 않는 이벤트 조기 감지
- ✅ 서비스별 독립적인 배포 가능
- ✅ 스키마 진화 과정 추적 가능
- ✅ 운영 중 버전 불일치 모니터링

---

## EventVersion Value Object

### 파일 위치
```
src/main/kotlin/com/stark/shoot/domain/shared/event/EventVersion.kt
```

### 구조

```kotlin
@JvmInline
value class EventVersion private constructor(val value: String) {
    companion object {
        // 각 이벤트 타입의 현재 버전
        val MESSAGE_SENT_V1 = EventVersion("1.0.0")
        val USER_CREATED_V1 = EventVersion("1.0.0")
        val FRIEND_ADDED_V1 = EventVersion("1.0.0")
        // ...

        fun from(version: String): EventVersion
        fun of(major: Int, minor: Int, patch: Int): EventVersion
    }

    val major: Int  // Breaking changes
    val minor: Int  // Backward-compatible additions
    val patch: Int  // Bug fixes

    fun isCompatibleWith(other: EventVersion): Boolean
    fun isNewerThan(other: EventVersion): Boolean
    fun isOlderThan(other: EventVersion): Boolean
}
```

### Semantic Versioning 규칙

| 변경 타입 | 버전 변경 | 호환성 | 예시 |
|----------|----------|--------|------|
| **Bug Fix** | PATCH +1 | 호환 O | 필드 검증 로직 수정 |
| **새 필드 추가** | MINOR +1 | 호환 O | `phoneNumber` 필드 추가 (Optional) |
| **필수 필드 추가** | MAJOR +1 | 호환 X | 새 필수 필드 `requiredField` 추가 |
| **필드 제거** | MAJOR +1 | 호환 X | `oldField` 필드 삭제 |
| **타입 변경** | MAJOR +1 | 호환 X | `age: Int` → `age: String` |

### 사용 예시

```kotlin
// 1. 기본 사용 (상수)
val version = EventVersion.MESSAGE_SENT_V1

// 2. 문자열로 생성
val version = EventVersion.from("1.2.3")

// 3. 숫자로 생성
val version = EventVersion.of(major = 1, minor = 2, patch = 3)

// 4. 버전 비교
val v1 = EventVersion.from("1.0.0")
val v2 = EventVersion.from("1.1.0")

v1.isCompatibleWith(v2)  // true (Major 버전 동일)
v2.isNewerThan(v1)       // true
v1.isOlderThan(v2)       // true
```

---

## Event Listener에서 버전 체크

### EventVersionValidator

**파일 위치:**
```
src/main/kotlin/com/stark/shoot/domain/shared/event/EventVersionValidator.kt
```

**주요 메서드:**

```kotlin
object EventVersionValidator {
    /**
     * 버전 호환성 체크 (Boolean 반환)
     * 호환되지 않으면 경고 로그를 남기고 false 반환
     */
    fun isSupported(
        event: DomainEvent,
        expectedVersion: EventVersion,
        consumerName: String
    ): Boolean

    /**
     * 버전 체크만 수행 (로깅 전용)
     * 호환되지 않아도 예외를 던지지 않음
     */
    fun checkAndLog(
        event: DomainEvent,
        expectedVersion: EventVersion,
        consumerName: String
    )

    /**
     * 정확히 일치하는지 확인 (엄격한 체크)
     */
    fun isExactMatch(
        event: DomainEvent,
        expectedVersion: EventVersion
    ): Boolean

    /**
     * 여러 버전 중 하나라도 호환되는지 확인
     */
    fun isSupportedAny(
        event: DomainEvent,
        supportedVersions: List<EventVersion>,
        consumerName: String
    ): Boolean
}
```

### Event Listener 패턴

#### 패턴 1: checkAndLog (권장)

```kotlin
@ApplicationEventListener
class MessageSentEventListener(/* dependencies */) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMessageSent(event: MessageSentEvent) {
        // 1️⃣ 버전 체크 (로깅만, 예외 없음)
        EventVersionValidator.checkAndLog(
            event = event,
            expectedVersion = EventVersion.MESSAGE_SENT_V1,
            consumerName = "MessageSentEventListener"
        )

        // 2️⃣ 이벤트 처리 로직
        logger.info { "Processing message sent event..." }
        // ...
    }
}
```

**로그 출력 예시:**
```
# 호환되는 경우 (1.0.0 → 1.1.0)
[INFO] MessageSentEventListener: Received newer event version. Expected: 1.0.0, Received: 1.1.0. Consider upgrading consumer.

# 호환되지 않는 경우 (1.0.0 → 2.0.0)
[WARN] MessageSentEventListener: Incompatible event version detected. Expected: 1.0.0, Received: 2.0.0. Event type: MessageSentEvent
```

#### 패턴 2: isSupported (조건부 처리)

```kotlin
@EventListener
fun handleMessageSent(event: MessageSentEvent) {
    // 버전 체크 후 조건부 처리
    if (!EventVersionValidator.isSupported(event, EventVersion.MESSAGE_SENT_V1, "MessageSentEventListener")) {
        logger.error { "Skipping incompatible event version: ${event.version}" }
        return  // 호환되지 않으면 처리 중단
    }

    // 호환되는 경우에만 처리
    processEvent(event)
}
```

#### 패턴 3: 여러 버전 지원

```kotlin
@EventListener
fun handleUserCreated(event: UserCreatedEvent) {
    val supportedVersions = listOf(
        EventVersion.USER_CREATED_V1,  // 1.x.x
        EventVersion.from("2.0.0")     // 2.x.x
    )

    if (!EventVersionValidator.isSupportedAny(event, supportedVersions, "UserCreatedEventListener")) {
        logger.warn { "Unsupported event version: ${event.version}" }
        return
    }

    // 버전별 분기 처리
    when (event.version.major) {
        1 -> handleV1(event)
        2 -> handleV2(event)
    }
}

private fun handleV1(event: UserCreatedEvent) {
    // v1 스키마 처리 로직
}

private fun handleV2(event: UserCreatedEvent) {
    // v2 스키마 처리 로직 (새 필드 포함)
}
```

---

## Event Schema 진화 전략

### 1. Minor Version 업데이트 (하위 호환)

**시나리오:** 새 Optional 필드 추가

```kotlin
// Before (1.0.0)
data class UserCreatedEvent(
    override val version: EventVersion = EventVersion.USER_CREATED_V1,
    val userId: UserId,
    val username: String,
    val nickname: String,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent

// After (1.1.0)
data class UserCreatedEvent(
    override val version: EventVersion = EventVersion.from("1.1.0"),
    val userId: UserId,
    val username: String,
    val nickname: String,
    val phoneNumber: String? = null,  // 새 Optional 필드
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent
```

**EventVersion.kt 업데이트:**
```kotlin
companion object {
    // 기존
    // val USER_CREATED_V1 = EventVersion("1.0.0")

    // 새 버전
    val USER_CREATED_V1 = EventVersion("1.1.0")  // Minor 버전 업데이트
}
```

**Consumer 대응:**
- ✅ 기존 Consumer는 수정 없이 동작 (phoneNumber 무시)
- ✅ 새 Consumer는 phoneNumber 활용 가능
- ✅ Major 버전 동일 (1.x.x) → 호환 O

### 2. Major Version 업데이트 (Breaking Change)

**시나리오:** 필수 필드 추가 또는 타입 변경

```kotlin
// Before (1.1.0)
data class UserCreatedEvent(
    override val version: EventVersion = EventVersion.from("1.1.0"),
    val userId: UserId,
    val username: String,
    val nickname: String,
    val phoneNumber: String? = null,
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent

// After (2.0.0) - Breaking Change
data class UserCreatedEvent(
    override val version: EventVersion = EventVersion.from("2.0.0"),
    val userId: UserId,
    val username: String,
    val nickname: String,
    val phoneNumber: String,  // Optional → Required (Breaking!)
    val email: String,        // 새 Required 필드 (Breaking!)
    override val occurredOn: Long = System.currentTimeMillis()
) : DomainEvent
```

**EventVersion.kt 업데이트:**
```kotlin
companion object {
    val USER_CREATED_V1 = EventVersion("1.1.0")
    val USER_CREATED_V2 = EventVersion("2.0.0")  // 새 Major 버전
}
```

**Consumer 대응 (필수):**
```kotlin
@EventListener
fun handleUserCreated(event: UserCreatedEvent) {
    val supportedVersions = listOf(
        EventVersion.USER_CREATED_V1,  // 1.x.x 지원
        EventVersion.USER_CREATED_V2   // 2.x.x 지원
    )

    if (!EventVersionValidator.isSupportedAny(event, supportedVersions, "UserCreatedEventListener")) {
        logger.warn { "Unsupported version: ${event.version}" }
        return
    }

    // 버전별 분기 처리
    when (event.version.major) {
        1 -> handleV1Legacy(event)
        2 -> handleV2(event)
    }
}
```

---

## Breaking Changes 처리

### 마이그레이션 전략

#### 1단계: 새 버전 배포 (Producer)

```kotlin
// User Service: v2.0.0 이벤트 발행 시작
val event = UserCreatedEvent(
    version = EventVersion.USER_CREATED_V2,  // 2.0.0
    userId = userId,
    username = username,
    nickname = nickname,
    phoneNumber = phoneNumber,  // 이제 필수
    email = email               // 새 필수 필드
)
eventPublisher.publish(event)
```

#### 2단계: Consumer 업데이트 (모든 Consumers)

```kotlin
// Notification Service: v2.0.0 이벤트 처리 준비
@EventListener
fun handleUserCreated(event: UserCreatedEvent) {
    val supportedVersions = listOf(
        EventVersion.USER_CREATED_V1,  // 기존 버전 유지
        EventVersion.USER_CREATED_V2   // 새 버전 추가
    )

    EventVersionValidator.isSupportedAny(event, supportedVersions, "UserCreatedEventListener")

    // 버전별 처리
    when (event.version.major) {
        1 -> sendWelcomeEmail(event.username, defaultEmail = "noreply@example.com")
        2 -> sendWelcomeEmail(event.username, event.email)  // 새 필드 사용
    }
}
```

#### 3단계: 모니터링

```kotlin
// 로그 모니터링으로 v1 이벤트 수신 여부 확인
[INFO] UserCreatedEventListener: Received older event version. Expected: 2.0.0, Received: 1.1.0. Backward compatibility maintained.

// v1 이벤트가 0이 되면 v1 처리 로직 제거 가능
```

#### 4단계: Legacy 코드 제거

```kotlin
// 모든 Consumer가 v2 이벤트만 받는 것을 확인 후
@EventListener
fun handleUserCreated(event: UserCreatedEvent) {
    // v1 분기 처리 로직 제거
    EventVersionValidator.checkAndLog(event, EventVersion.USER_CREATED_V2, "UserCreatedEventListener")

    // v2 로직만 유지
    sendWelcomeEmail(event.username, event.email)
}
```

---

## Best Practices

### 1. 버전 체크는 항상 핸들러 최상단에 배치

```kotlin
// ✅ Good
@EventListener
fun handleEvent(event: SomeEvent) {
    EventVersionValidator.checkAndLog(event, EventVersion.SOME_EVENT_V1, "SomeEventListener")
    // 이후 처리 로직
}

// ❌ Bad
@EventListener
fun handleEvent(event: SomeEvent) {
    val data = event.someField
    processData(data)
    EventVersionValidator.checkAndLog(...)  // 너무 늦음!
}
```

### 2. Consumer Name은 명확하게

```kotlin
// ✅ Good - 클래스명 그대로 사용
EventVersionValidator.checkAndLog(event, expectedVersion, "MessageSentEventListener")

// ❌ Bad - 모호한 이름
EventVersionValidator.checkAndLog(event, expectedVersion, "Consumer")
```

### 3. Breaking Change는 신중하게

```kotlin
// ✅ Good - Optional 필드로 추가
data class Event(
    val newField: String? = null  // Minor 업데이트
)

// ⚠️ Caution - Required 필드는 Major 업데이트
data class Event(
    val newField: String  // Breaking Change!
)
```

### 4. 버전 상수는 EventVersion.kt에서 관리

```kotlin
// ✅ Good
companion object {
    val MESSAGE_SENT_V1 = EventVersion("1.0.0")
    val MESSAGE_SENT_V2 = EventVersion("2.0.0")
}

// ❌ Bad - 이벤트 클래스에 하드코딩
data class MessageSentEvent(
    override val version: EventVersion = EventVersion.from("1.0.0")  // 매직 넘버!
)
```

### 5. 로그 레벨 활용

```kotlin
// EventVersionValidator 내부 로그 레벨
- WARN: 호환되지 않는 버전 (Major 버전 불일치)
- INFO: 새 버전 수신 (Consumer 업그레이드 권장)
- DEBUG: 오래된 버전 수신 (하위 호환 유지됨)
```

### 6. Event Upcasting 고려 (향후 확장)

```kotlin
// v1 이벤트를 v2로 변환하는 Upcaster
object UserCreatedEventUpcaster {
    fun upcastV1toV2(v1Event: UserCreatedEventV1): UserCreatedEventV2 {
        return UserCreatedEventV2(
            version = EventVersion.USER_CREATED_V2,
            userId = v1Event.userId,
            username = v1Event.username,
            nickname = v1Event.nickname,
            phoneNumber = v1Event.phoneNumber ?: "",  // 기본값 제공
            email = "unknown@example.com"  // 기본값 제공
        )
    }
}
```

---

## FAQ

### Q1. 모든 Event Listener에 버전 체크를 추가해야 하나요?

**A:** 네, 모든 Event Listener에 추가하는 것이 권장됩니다.
특히 MSA 환경에서는 서비스 간 배포 시점이 다르므로, 버전 불일치 가능성이 항상 존재합니다.

### Q2. `checkAndLog`와 `isSupported`의 차이는?

| 메서드 | 반환값 | 용도 |
|--------|-------|-----|
| `checkAndLog` | void | 로깅만 수행, 처리 계속 |
| `isSupported` | Boolean | 조건부 처리 (return 가능) |

- **checkAndLog**: 경고 로그만 남기고 이벤트 처리를 계속하고 싶을 때
- **isSupported**: 호환되지 않으면 처리를 중단하고 싶을 때

### Q3. Major 버전이 다르면 무조건 처리를 중단해야 하나요?

**A:** 아니요, 상황에 따라 다릅니다.

```kotlin
// 1. 엄격한 처리 (중단)
if (!EventVersionValidator.isSupported(event, expectedVersion, "Listener")) {
    return  // 호환되지 않으면 중단
}

// 2. 유연한 처리 (여러 버전 지원)
val supportedVersions = listOf(v1, v2, v3)
if (EventVersionValidator.isSupportedAny(event, supportedVersions, "Listener")) {
    when (event.version.major) {
        1 -> handleV1(event)
        2 -> handleV2(event)
        3 -> handleV3(event)
    }
}
```

### Q4. Event 스키마 변경 시 체크리스트는?

1. ✅ 변경 타입 결정 (Minor vs Major)
2. ✅ EventVersion 상수 업데이트
3. ✅ Event 클래스 수정
4. ✅ 영향받는 모든 Consumer 확인
5. ✅ Consumer 업데이트 (Major 변경 시 필수)
6. ✅ 배포 계획 수립 (Producer → All Consumers)
7. ✅ 모니터링 (로그 확인)
8. ✅ Legacy 코드 정리 (필요시)

### Q5. Kafka 등 Message Broker 사용 시 주의사항은?

**A:** Kafka는 메시지를 오래 보관할 수 있으므로, **오래된 버전의 이벤트도 재처리될 수 있습니다.**

```kotlin
// Kafka Consumer Replay 대비
@KafkaListener(topics = ["user-events"])
fun consume(event: UserCreatedEvent) {
    // 여러 버전 지원 필수!
    val supportedVersions = listOf(
        EventVersion.USER_CREATED_V1,
        EventVersion.USER_CREATED_V2
    )

    EventVersionValidator.isSupportedAny(event, supportedVersions, "KafkaConsumer")

    when (event.version.major) {
        1 -> handleV1(event)
        2 -> handleV2(event)
    }
}
```

---

## 참고 자료

- **EventVersion.kt**: `src/main/kotlin/com/stark/shoot/domain/shared/event/EventVersion.kt`
- **EventVersionValidator.kt**: `src/main/kotlin/com/stark/shoot/domain/shared/event/EventVersionValidator.kt`
- **DomainEvent.kt**: `src/main/kotlin/com/stark/shoot/domain/shared/event/event/DomainEvent.kt`
- **Event Listeners**: `src/main/kotlin/com/stark/shoot/application/service/event/**/*EventListener.kt`

---

**Last Updated:** 2025-11-08
**Task:** TASK-011 (Event Versioning 구현)
