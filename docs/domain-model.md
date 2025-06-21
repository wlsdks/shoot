# Shoot 도메인 모델 개요

이 문서는 Shoot 프로젝트의 핵심 도메인 모델을 정리한 문서입니다. 각 엔티티의 역할과 주요 필드를 간략히 설명하여 도메인 구조를 이해하기 쉽게 합니다. 실제 구현 세부 사항은 코드와 다른 문서를 참고하세요.

## 목차
1. [도메인 요약](#도메인-요약)
2. [주요 엔티티](#주요-엔티티)
    - [ChatMessage](#chatmessage)
    - [ChatRoom](#chatroom)
    - [Notification](#notification)
    - [User](#user)
    - [FriendRequest](#friendrequest)
    - [FriendGroup](#friendgroup)
    - [RefreshToken](#refreshtoken)
3. [도메인 이벤트](#도메인-이벤트)
4. [도메인 간 관계](#도메인-간-관계)

## 도메인 요약

프로젝트는 크게 **채팅(chat)**, **채팅방(chatroom)**, **알림(notification)**, **사용자(user)** 도메인으로 구성됩니다. 각 도메인은 독립적인 비즈니스 로직을 갖고 있으며 도메인 이벤트를 통해 서로 상호작용합니다.

## 주요 엔티티

### ChatMessage
채팅 메시지에 대한 정보를 담는 애그리게이트입니다. 메시지 상태, 내용, 반응 정보 등을 포함합니다.

주요 필드:
- `id`: 메시지 식별자
- `roomId`: 메시지가 속한 채팅방 ID
- `senderId`: 발신자 ID
- `content`: 메시지 내용(`text`, `type` 등)
- `status`: 메시지 상태(`SENDING`, `SAVED` 등)
- `replyToMessageId`: 답장 대상 메시지 ID
- `threadId`: 스레드(루트 메시지) ID
- `messageReactions`: 메시지에 달린 이모지 반응들
- `mentions`: 멘션된 사용자 목록
- `isPinned`: 채팅방에서 고정되었는지 여부
- `readBy`: 사용자별 읽음 상태

`ChatMessage`는 메시지 읽음 처리, 수정, 삭제, 리액션 토글, URL 미리보기 처리 등 다양한 도메인 로직을 제공합니다.

### ChatRoom
사용자들이 대화를 나누는 공간을 나타내며, 참여자 목록과 채팅방 설정을 관리합니다.

주요 필드:
- `id`: 채팅방 식별자
- `title`: 채팅방 제목
- `type`: 채팅방 타입(`INDIVIDUAL` 또는 `GROUP`)
- `participants`: 참여자 ID 목록
- `lastMessageId`: 마지막 메시지 ID
- `announcement`: 채팅방 공지사항
- `pinnedParticipants`: 채팅방을 즐겨찾기한 사용자들

주요 기능으로는 참여자 추가/제거, 즐겨찾기 관리, 공지사항 업데이트, 채팅방이 비어 있는지 확인 등이 있습니다.

### Notification
사용자에게 전달되는 알림 정보를 캡슐화합니다. 알림은 다양한 이벤트로부터 생성될 수 있습니다.

주요 필드:
- `id`: 알림 식별자
- `userId`: 알림 수신자 ID
- `title`: 알림 제목
- `message`: 알림 내용
- `type`: 알림 유형
- `sourceId`: 알림 발생 원천 ID
- `sourceType`: 알림 원천 타입(`CHAT`, `SYSTEM` 등)
- `isRead`: 읽음 여부
- `metadata`: 추가 메타데이터

알림은 읽음 처리 또는 삭제 처리 등의 도메인 로직을 포함합니다.

### User
시스템의 사용자를 나타내는 엔티티입니다. 계정 정보와 친구 관계, 프로필 정보를 관리합니다.

주요 필드:
- `id`: 사용자 식별자
- `username`: 로그인에 사용되는 이름
- `nickname`: 표시용 닉네임
- `status`: 온라인 상태
- `userCode`: 친구 추가용 고유 코드
- `profileImageUrl`, `backgroundImageUrl`: 프로필 이미지 정보
- `friendIds`: 친구 ID 목록
- `incomingFriendRequestIds`, `outgoingFriendRequestIds`: 친구 요청 목록
- `blockedUserIds`: 차단한 사용자 목록

`User`는 친구 추가/삭제, 프로필 수정, 차단/차단 해제 등 다양한 동작을 제공합니다.

### FriendRequest
사용자 간 친구 요청을 표현합니다.

주요 필드:
- `id`: 요청 식별자
- `senderId`: 요청을 보낸 사용자
- `receiverId`: 요청을 받은 사용자
- `status`: 현재 상태(`PENDING`, `ACCEPTED` 등)

상태 변화에 따라 `accept`, `reject`, `cancel`과 같은 메서드가 제공됩니다.

### FriendGroup
친구를 그룹화하여 관리하기 위한 애그리게이트입니다.

주요 필드:
- `id`: 그룹 식별자
- `ownerId`: 그룹을 소유한 사용자
- `name`: 그룹 이름
- `memberIds`: 그룹에 속한 친구 목록

그룹 이름 변경, 설명 업데이트, 멤버 추가/제거 등의 기능을 포함합니다.

### RefreshToken
로그인 세션 유지를 위한 리프레시 토큰 정보를 보관합니다.

주요 필드:
- `id`: 토큰 식별자
- `userId`: 토큰 소유자
- `token`: 실제 리프레시 토큰 값
- `expirationDate`: 만료 시각
- `deviceInfo`, `ipAddress`: 발급된 기기 및 IP 정보

만료 여부 확인, 사용 시각 갱신, 토큰 폐기 등의 로직을 제공합니다.

## 도메인 이벤트

각 도메인은 도메인 이벤트를 통해 서로 느슨하게 결합되어 있습니다. 주요 이벤트 예시는 다음과 같습니다.
- `NewMessageEvent`: 새 메시지 생성 시 발생하여 알림 시스템 등에서 사용
- `MessageReactionEvent`: 메시지에 반응이 추가/제거될 때 발생
- `ChatRoomCreatedEvent`: 채팅방 생성 시 발행

이벤트들은 `DomainEvent` 인터페이스를 구현하며, 이벤트 리스너에서 비동기로 처리됩니다.

## 도메인 간 관계

- **채팅 → 알림**: 메시지 작성이나 멘션, 반응 등의 이벤트가 발생하면 알림 도메인이 해당 이벤트를 수신하여 `Notification`을 생성합니다.
- **채팅 ↔ 채팅방**: 메시지가 생성되면 채팅방의 마지막 메시지 정보가 갱신됩니다. 채팅방에서 메시지를 고정하면 `ChatMessage`의 고정 상태가 변경됩니다.
- **사용자 ↔ 친구/친구요청**: `User`는 `FriendRequest`와 `FriendGroup`을 통해 다른 사용자와의 관계를 관리합니다.

이와 같은 관계를 통해 도메인들은 서로 영향을 주고받으며, 헥사고날 아키텍처를 통해 외부 의존성과는 분리되어 있습니다.

---
이 문서는 코드와 기존 문서들을 토대로 도메인 모델의 큰 틀을 설명합니다. 세부적인 구현이나 추가 기능은 [DDD 아키텍처 문서](ddd-architecture.md)와 각종 구현 문서를 참고하시기 바랍니다.
