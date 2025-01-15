MongoDB에서 어떻게 저장되고 응답되는지 보여드리겠습니다.

```bash
# 1. ChatMessageDocument 저장/조회 예시
> db.messages.insertOne({
  roomId: ObjectId("5f7d3a2e9d3e2a1234567890"),
  senderId: ObjectId("5f7d3a2e9d3e2a1234567891"),
  content: {
    text: "안녕하세요",
    type: "TEXT",
    metadata: {
      urlPreview: null,
      readAt: null
    },
    attachments: [],
    isEdited: false,
    isDeleted: false
  },
  status: "SENT",
  replyToMessageId: null,
  reactions: {},
  mentions: [],
  createdAt: ISODate("2024-01-15T10:00:00Z"),
  updatedAt: null
})

# 2. ChatRoomDocument 저장/조회 예시
> db.chat_rooms.insertOne({
  participants: [
    ObjectId("5f7d3a2e9d3e2a1234567891"),
    ObjectId("5f7d3a2e9d3e2a1234567892")
  ],
  lastMessageId: ObjectId("5f7d3a2e9d3e2a1234567893"),
  metadata: {
    title: "프로젝트 논의방",
    type: "INDIVIDUAL",
    participantsMetadata: {
      "5f7d3a2e9d3e2a1234567891": {
        lastReadMessageId: ObjectId("5f7d3a2e9d3e2a1234567893"),
        lastReadAt: ISODate("2024-01-15T10:00:00Z"),
        joinedAt: ISODate("2024-01-01T00:00:00Z"),
        role: "OWNER",
        nickname: null,
        isActive: true
      },
      "5f7d3a2e9d3e2a1234567892": {
        lastReadMessageId: ObjectId("5f7d3a2e9d3e2a1234567892"),
        lastReadAt: ISODate("2024-01-15T09:55:00Z"),
        joinedAt: ISODate("2024-01-01T00:00:00Z"),
        role: "MEMBER",
        nickname: null,
        isActive: true
      }
    },
    settings: {
      isNotificationEnabled: true,
      retentionDays: null,
      isEncrypted: false,
      customSettings: {}
    }
  },
  lastActiveAt: ISODate("2024-01-15T10:00:00Z"),
  createdAt: ISODate("2024-01-01T00:00:00Z"),
  updatedAt: null
})

# 3. UserDocument 저장/조회 예시
> db.users.insertOne({
  username: "john.doe",
  nickname: "John",
  status: "ONLINE",
  profileImageUrl: "https://example.com/profile.jpg",
  lastSeenAt: ISODate("2024-01-15T10:00:00Z"),
  createdAt: ISODate("2024-01-01T00:00:00Z"),
  updatedAt: null
})

# 채팅방 조회 예시 (인덱스 활용)
> db.chat_rooms.find({
  participants: ObjectId("5f7d3a2e9d3e2a1234567891")
}).sort({ lastActiveAt: -1 })

# 메시지 조회 예시 (인덱스 활용)
> db.messages.find({
  roomId: ObjectId("5f7d3a2e9d3e2a1234567890")
}).sort({ createdAt: -1 })
```

이 구조의 주요 장점들:

1. 임베디드 문서(embedded documents) 활용:
    - `MessageContentDocument`, `ChatRoomMetadataDocument` 등이 중첩 구조로 저장되어 조회가 효율적
    - 관련 데이터를 단일 쿼리로 가져올 수 있음

2. 효과적인 인덱싱:
    - `room_created_idx`: 채팅방별 메시지 조회 최적화
    - `sender_created_idx`: 발신자별 메시지 조회 최적화
    - `participants_idx`: 사용자별 채팅방 조회 최적화

3. 유연한 확장성:
    - `customSettings`와 같은 필드로 추가 설정 확장 가능
    - `metadata` 맵을 통한 동적 데이터 저장 가능