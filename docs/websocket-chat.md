

# WebSocket 채팅 구조 설명

질문에서 언급된 `/app/chat` 경로는 STOMP 메시지 브로커에서 사용되는 목적지(destination) 경로입니다. 이 경로가 어떻게 작동하는지 설명해 드리겠습니다.

## WebSocket 연결 구조

1. **클라이언트 연결 엔드포인트**:
   ```javascript
   const socket = new SockJS(`http://localhost:8100/ws/chat?token=${token}`);
   ```
    - 프론트엔드는 `/ws/chat` 엔드포인트로 WebSocket 연결을 시작합니다.
    - 이 엔드포인트는 `WebSocketConfig.kt`에서 설정되어 있습니다.

2. **메시지 브로커 설정**:
   ```kotlin
   registry.setApplicationDestinationPrefixes("/app")
   ```
    - `WebSocketConfig.kt`에서 애플리케이션 목적지 접두사를 `/app`으로 설정했습니다.
    - 이는 클라이언트가 서버로 메시지를 보낼 때 사용하는 접두사입니다.

3. **메시지 핸들러**:
   ```kotlin
   @MessageMapping("/chat")
   fun handleChatMessage(message: ChatMessageRequest) {
       val command = SendMessageCommand.of(message)
       sendMessageUseCase.sendMessage(command)
   }
   ```
    - `SendMessageStompHandler.kt`에서 `/chat` 경로에 대한 메시지 핸들러가 정의되어 있습니다.
    - 접두사(`/app`)와 매핑 경로(`/chat`)를 합치면 `/app/chat`이 됩니다.

4. **메시지 인터셉터**:
    - `StompChannelInterceptor.kt`에서 `/app/chat` 경로로 들어오는 메시지를 인터셉트하여 유효성을 검사합니다.

## 메시지 흐름

1. 클라이언트가 `/ws/chat` 엔드포인트로 WebSocket 연결을 설정합니다.
2. 클라이언트가 `/app/chat` 목적지로 메시지를 전송합니다.
3. `StompChannelInterceptor`가 메시지를 인터셉트하여 유효성을 검사합니다.
4. 유효성 검사를 통과하면 `SendMessageStompHandler`의 `handleChatMessage` 메서드가 호출됩니다.
5. 메시지는 `SendMessageCommand`로 변환되어 `sendMessageUseCase.sendMessage(command)`를 통해 처리됩니다.
6. 처리된 메시지는 Redis를 통해 실시간으로 브로드캐스트되고, Kafka를 통해 영속화됩니다.

따라서 질문에서 언급된 코드는 정확히 WebSocket 채팅 기능을 구현하는 부분입니다. `/app/chat`은 STOMP 프로토콜에서 사용되는 목적지 경로로, 클라이언트가 서버로 채팅 메시지를 보낼 때 사용됩니다.