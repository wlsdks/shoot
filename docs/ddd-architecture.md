# Domain-Driven Design (DDD) 아키텍처 설계

이 문서는 Shoot 프로젝트의 Domain-Driven Design (DDD) 아키텍처 설계에 대한 설명입니다. 각 도메인의 역할, 애그리게이트 분리 방식, 그리고 비즈니스 로직에 대해 설명합니다.

## 목차

1. [아키텍처 개요](#아키텍처-개요)
2. [도메인 구조](#도메인-구조)
3. [주요 애그리게이트](#주요-애그리게이트)
4. [도메인 간 상호작용](#도메인-간-상호작용)
5. [헥사고날 아키텍처 적용](#헥사고날-아키텍처-적용)

## 아키텍처 개요

Shoot 프로젝트는 DDD(Domain-Driven Design)와 헥사고날 아키텍처(Hexagonal Architecture)를 결합하여 설계되었습니다. 이 설계 방식은 다음과 같은 이점을 제공합니다:

- **도메인 중심 설계**: 비즈니스 도메인을 중심으로 시스템을 설계하여 비즈니스 로직의 명확성을 높임
- **경계 분리**: 도메인 로직과 인프라스트럭처 코드를 명확히 분리하여 유지보수성 향상
- **테스트 용이성**: 도메인 로직을 외부 의존성으로부터 분리하여 단위 테스트 용이성 확보
- **유연한 확장성**: 새로운 기능이나 인터페이스를 추가할 때 기존 코드 변경 최소화

프로젝트 구조는 다음과 같은 주요 레이어로 구성됩니다:

- **도메인 레이어**: 핵심 비즈니스 로직과 엔티티를 포함
- **애플리케이션 레이어**: 유스케이스 구현 및 도메인 서비스 조정
- **어댑터 레이어**: 외부 시스템과의 통신 담당 (웹, 데이터베이스, 메시징 등)
- **인프라스트럭처 레이어**: 기술적 구현 세부사항 제공

## 도메인 구조

프로젝트는 다음과 같은 주요 도메인으로 구성되어 있습니다:

### 1. 채팅 도메인 (chat)

채팅 도메인은 다음과 같은 하위 도메인으로 구성됩니다:

- **메시지 (message)**: 채팅 메시지와 관련된 모든 개념
- **채팅방 (room)**: 채팅방과 참여자 관리
- **이벤트 (event)**: 채팅 관련 도메인 이벤트
- **반응 (reaction)**: 메시지에 대한 반응 (이모지 등)
- **사용자 (user)**: 채팅 컨텍스트 내의 사용자 관련 개념

### 2. 알림 도메인 (notification)

알림 도메인은 다음과 같은 개념을 다룹니다:

- **알림 (Notification)**: 사용자에게 전달되는 알림 메시지
- **알림 유형 (NotificationType)**: 다양한 알림 유형 분류
- **소스 유형 (SourceType)**: 알림의 출처 정의
- **알림 이벤트 (event)**: 알림 관련 도메인 이벤트

### 3. 공통 도메인 (common)

여러 도메인에서 공유하는 공통 개념과 유틸리티를 포함합니다.

### 4. 예외 도메인 (exception)

도메인별 예외 정의와 처리 로직을 포함합니다.

## 주요 애그리게이트

### 1. 채팅 메시지 애그리게이트 (ChatMessage)

`ChatMessage`는 채팅 메시지와 관련된 모든 정보와 동작을 캡슐화합니다.

**주요 속성**:
- id, roomId, senderId: 기본 식별자
- content: 메시지 내용 (텍스트, 타입 등)
- status: 메시지 상태 (SENDING, SAVED 등)
- messageReactions: 메시지에 대한 반응
- mentions: 메시지 내 언급된 사용자
- readBy: 메시지를 읽은 사용자 추적
- metadata: 메시지 관련 추가 메타데이터
- isPinned, pinnedBy, pinnedAt: 고정 메시지 관련 정보

**주요 비즈니스 로직**:
- `markAsRead`: 메시지를 읽음 처리
- `updatePinStatus`: 메시지 고정 상태 업데이트
- `pinMessageInRoom`: 채팅방에 메시지 고정 (한 채팅방에는 최대 하나의 고정 메시지만 존재)
- `editMessage`: 메시지 내용 수정 (텍스트 타입만 수정 가능)
- `markAsDeleted`: 메시지 삭제 처리
- `toggleReaction`: 메시지에 대한 반응 토글
- `setUrlPreview`/`markNeedsUrlPreview`: URL 미리보기 처리

**팩토리 메서드**:
- `create`: 새 메시지 생성
- `processUrlPreview`: 메시지 내 URL 미리보기 처리
- `fromRequest`: 요청 DTO로부터 메시지 생성

### 2. 채팅방 애그리게이트 (ChatRoom)

`ChatRoom`은 채팅방과 참여자 관리에 관한 모든 정보와 동작을 캡슐화합니다.

**주요 속성**:
- id, title, type: 기본 식별자와 정보
- participants: 채팅방 참여자 목록
- lastMessageId: 마지막 메시지 참조
- pinnedParticipants: 채팅방을 고정한 사용자
- announcement: 채팅방 공지사항

**주요 비즈니스 로직**:
- `calculateParticipantChanges`: 참여자 변경사항 계산
- `addParticipant`/`removeParticipant`: 참여자 추가/제거
- `updateParticipants`: 참여자 목록 업데이트
- `updateFavoriteStatus`: 즐겨찾기 상태 업데이트 (최대 5개 제한)
- `updateAnnouncement`: 공지사항 업데이트
- `isEmpty`/`shouldBeDeleted`: 채팅방 비어있음/삭제 여부 확인
- `isDirectChatBetween`: 1:1 채팅방 확인
- `createChatRoomTitle`: 채팅방 제목 생성
- `formatTimestamp`: 타임스탬프 포맷팅

**팩토리 메서드**:
- `createDirectChat`: 1:1 채팅방 생성
- `findDirectChatBetween`: 두 사용자 간 1:1 채팅방 찾기

### 3. 알림 애그리게이트 (Notification)

`Notification`은 사용자에게 전달되는 알림과 관련된 모든 정보와 동작을 캡슐화합니다.

**주요 속성**:
- id, userId, title, message: 기본 식별자와 내용
- type: 알림 유형
- sourceId, sourceType: 알림 출처 정보
- isRead, readAt: 읽음 상태 추적
- isDeleted, deletedAt: 삭제 상태 추적
- metadata: 추가 메타데이터

**주요 비즈니스 로직**:
- `markAsRead`: 알림을 읽음 처리
- `belongsToUser`: 알림 소유권 확인
- `validateOwnership`: 알림 소유권 검증
- `markAsDeleted`: 알림 삭제 처리

**팩토리 메서드**:
- `fromChatEvent`: 채팅 이벤트로부터 알림 생성
- `fromEvent`: 알림 이벤트로부터 알림 생성
- `create`: 일반 알림 생성

## 도메인 간 상호작용

도메인 간 상호작용은 주로 이벤트를 통해 이루어집니다. 예를 들어:

1. **채팅 → 알림**: 새 메시지가 생성되면 `ChatEvent`가 발생하고, 이 이벤트는 알림 도메인에서 `Notification` 객체를 생성하는 데 사용됩니다.

2. **메시지 → 채팅방**: 메시지가 생성되면 해당 채팅방의 `lastMessageId`와 `lastActiveAt`이 업데이트됩니다.

3. **반응 → 알림**: 메시지에 반응이 추가되면 메시지 작성자에게 알림이 전송됩니다.

이러한 상호작용은 주로 애플리케이션 서비스 레이어에서 조정되며, 도메인 이벤트를 통해 느슨하게 결합됩니다.

## 헥사고날 아키텍처 적용

프로젝트는 헥사고날 아키텍처(포트 및 어댑터 패턴)를 적용하여 도메인 로직을 외부 의존성으로부터 분리합니다:

### 포트 (Ports)

1. **인바운드 포트 (Primary/Driving Ports)**:
   - 애플리케이션이 제공하는 유스케이스를 정의하는 인터페이스
   - 예: `SendMessageUseCase`, `GetMessagesUseCase`, `EditMessageUseCase`

2. **아웃바운드 포트 (Secondary/Driven Ports)**:
   - 애플리케이션이 필요로 하는 외부 서비스를 정의하는 인터페이스
   - 예: `KafkaMessagePublishPort`, `ExtractUrlPort`, `CacheUrlPreviewPort`

### 어댑터 (Adapters)

1. **인바운드 어댑터 (Primary/Driving Adapters)**:
   - 외부 요청을 애플리케이션 포트로 변환
   - 예: REST 컨트롤러, 웹소켓 핸들러

2. **아웃바운드 어댑터 (Secondary/Driven Adapters)**:
   - 애플리케이션 포트를 외부 서비스 호출로 변환
   - 예: 데이터베이스 리포지토리, Kafka 프로듀서, Redis 클라이언트

이러한 구조는 도메인 로직을 기술적 구현 세부사항으로부터 분리하여 코드의 유지보수성과 테스트 용이성을 높입니다.

## 결론

Shoot 프로젝트의 DDD 아키텍처는 비즈니스 도메인을 명확하게 모델링하고, 도메인 로직을 외부 의존성으로부터 분리하여 유지보수성과 확장성을 높이는 데 중점을 두고 있습니다. 각 도메인은 자체 비즈니스 규칙을 캡슐화하고, 도메인 간 상호작용은 주로 이벤트를 통해 이루어집니다. 헥사고날 아키텍처의 적용은 이러한 도메인 중심 설계를 기술적 구현 세부사항으로부터 보호하는 역할을 합니다.