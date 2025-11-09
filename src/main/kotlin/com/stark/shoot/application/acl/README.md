# Anti-Corruption Layer (ACL)

## 개요

이 패키지는 DDD의 Anti-Corruption Layer 패턴을 구현하여 **Bounded Context 간 경계를 명확히 관리**합니다.

## ACL의 역할

### 1. 도메인 보호 (Domain Protection)
- 외부 Context의 변경이 내부 도메인에 영향을 주지 않도록 방어
- 각 Context가 독립적인 도메인 모델을 유지하도록 보장

### 2. 타입 변환 (Type Translation)
- 서로 다른 Context 간 타입 변환 수행
- 구조적으로 동일하지만 타입이 다른 VO 간 변환
- 예: `ChatRoom.ChatRoomId` ↔ `Chat.ChatRoomId`

### 3. MSA 준비 (MSA Readiness)
- 향후 서비스 분리 시 API 경계에서 DTO 변환 역할 수행
- 서비스 간 통신 시 도메인 모델 직접 노출 방지

## 현재 구현

### ChatRoomIdConverter
Chat Context와 ChatRoom Context 간 ChatRoomId 변환을 담당합니다.

```kotlin
// ChatRoom Context → Chat Context
val chatContextId = chatRoomId.toChat()

// Chat Context → ChatRoom Context
val chatRoomContextId = chatId.toChatRoom()
```

## 사용 위치

ACL은 다음 계층에서 사용됩니다:

1. **Application Service Layer**: Context 간 도메인 객체 이동 시
2. **Event Listener**: 이벤트 처리 시 다른 Context 조회 필요 시
3. **Adapter Layer**: 외부 시스템과의 경계

## 향후 확장 계획

### Phase 1: 검증 로직 추가
```kotlin
fun toChat(chatRoomId: ChatRoomId): Chat.ChatRoomId {
    // 검증 로직
    require(chatRoomId.value > 0) { "Invalid ChatRoomId" }
    return Chat.ChatRoomId.from(chatRoomId.value)
}
```

### Phase 2: 복잡한 변환
```kotlin
// 단순 ID 변환을 넘어 복잡한 객체 변환
fun toChatMessage(externalMessage: ExternalMessage): ChatMessage {
    // 구조 변환, 필드 매핑, 검증 등
}
```

### Phase 3: MSA 서비스 간 변환
```kotlin
// 외부 서비스 응답 → 내부 도메인 모델
fun fromExternalApi(response: ExternalApiResponse): DomainModel {
    // API 응답을 내부 도메인 모델로 변환
    // 외부 API 변경이 내부 도메인에 영향을 주지 않도록 보호
}
```

## 설계 원칙

### 1. 방향성
- ACL은 단방향 또는 양방향으로 설계 가능
- 현재는 양방향 (Chat ↔ ChatRoom)
- MSA 환경에서는 주로 단방향 (External → Internal)

### 2. 위치
- **Application Layer**에 위치
- Domain Layer는 ACL을 알지 못함 (의존하지 않음)
- Adapter Layer는 ACL을 사용할 수 있음

### 3. 책임
- 변환 로직만 담당
- 비즈니스 로직은 포함하지 않음
- 검증은 최소한으로 (타입 안전성 검증 정도)

## 참고

- [DDD: Anti-Corruption Layer Pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer)
- Evans, Eric. "Domain-Driven Design" - Chapter 14: Maintaining Model Integrity
