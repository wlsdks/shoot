# Shoot API Documentation
# Shoot API 문서

This document provides a comprehensive guide to all available API endpoints in the Shoot application. Use this guide to interact with the application's backend services.
이 문서는 Shoot 애플리케이션에서 사용 가능한 모든 API 엔드포인트에 대한 종합적인 가이드를 제공합니다. 이 가이드를 사용하여 애플리케이션의 백엔드 서비스와 상호작용하세요.

## Base URL (기본 URL)

All REST API endpoints are prefixed with: `http://localhost:8100`
모든 REST API 엔드포인트는 다음 접두사로 시작합니다: `http://localhost:8100`

## Authentication (인증)

Most endpoints require authentication using JWT tokens. Include the token in the Authorization header:
대부분의 엔드포인트는 JWT 토큰을 사용한 인증이 필요합니다. Authorization 헤더에 토큰을 포함하세요:

```
Authorization: Bearer <your_jwt_token>
```

## REST API Endpoints (REST API 엔드포인트)

### User Management (사용자 관리)

#### Register User (사용자 등록)
- **URL**: `/api/v1/users`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Description**: Create a new user account
- **설명**: 새로운 사용자 계정을 생성합니다
- **Request Parameters (요청 매개변수)**:
  - `username` (string, required): User's username (사용자 아이디, 필수)
  - `nickname` (string, required): User's display name (사용자 표시 이름, 필수)
  - `password` (string, required): User's password (비밀번호, 필수)
  - `email` (string, required): User's email address (이메일 주소, 필수)
  - `bio` (string, optional): User's biography (자기소개, 선택사항)
  - `profileImage` (file, optional): User's profile image (프로필 이미지, 선택사항)
- **Example Request**:
```
curl -X POST http://localhost:8100/api/v1/users \
  -H "Content-Type: multipart/form-data" \
  -F "username=johndoe" \
  -F "nickname=John" \
  -F "password=securepassword" \
  -F "email=john@example.com" \
  -F "bio=Hello, I'm John" \
  -F "profileImage=@/path/to/image.jpg"
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "id": 1,
    "username": "johndoe",
    "nickname": "John",
    "email": "john@example.com",
    "bio": "Hello, I'm John",
    "profileImageUrl": "http://localhost:8100/images/profile/1.jpg"
  }
}
```

#### Delete User Account (회원 탈퇴)
- **URL**: `/api/v1/users/me`
- **Method**: `DELETE`
- **Description**: Delete the current user's account
- **설명**: 현재 사용자의 계정을 삭제합니다
- **Authentication**: Required (인증: 필수)
- **Example Request**:
```
curl -X DELETE http://localhost:8100/api/v1/users/me \
  -H "Authorization: Bearer <your_jwt_token>"
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": "회원 탈퇴가 완료되었습니다.",
  "data": null
}
```

### Authentication (인증)

#### Login (로그인)
- **URL**: `/api/v1/auth/login`
- **Method**: `POST`
- **Content-Type**: `application/json`
- **Description**: Authenticate user and get JWT token
- **설명**: 사용자를 인증하고 JWT 토큰을 발급받습니다
- **Request Body**:
```json
{
  "username": "johndoe",
  "password": "securepassword"
}
```
- **Example Request**:
```
curl -X POST http://localhost:8100/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","password":"securepassword"}'
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": "로그인에 성공했습니다.",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1
  }
}
```

#### Get Current User (현재 사용자 정보 조회)
- **URL**: `/api/v1/auth/me`
- **Method**: `GET`
- **Description**: Get current authenticated user's details
- **설명**: 현재 인증된 사용자의 상세 정보를 조회합니다
- **Authentication**: Required (인증: 필수)
- **Example Request**:
```
curl -X GET http://localhost:8100/api/v1/auth/me \
  -H "Authorization: Bearer <your_jwt_token>"
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": null,
  "data": {
    "id": 1,
    "username": "johndoe",
    "nickname": "John",
    "email": "john@example.com",
    "bio": "Hello, I'm John",
    "profileImageUrl": "http://localhost:8100/images/profile/1.jpg"
  }
}
```

### Chat Rooms (채팅방)

#### Create Direct Chat (1:1 채팅방 생성)
- **URL**: `/api/v1/chatrooms/create/direct`
- **Method**: `POST`
- **Description**: Create a 1:1 chat room with another user
- **설명**: 다른 사용자와 1:1 채팅방을 생성합니다
- **Authentication**: Required (인증: 필수)
- **Request Parameters (요청 매개변수)**:
  - `userId` (long, required): Current user's ID (현재 사용자 ID, 필수)
  - `friendId` (long, required): Friend's user ID (친구 사용자 ID, 필수)
- **Example Request**:
```
curl -X POST "http://localhost:8100/api/v1/chatrooms/create/direct?userId=1&friendId=2" \
  -H "Authorization: Bearer <your_jwt_token>"
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": "채팅방이 생성되었습니다.",
  "data": {
    "id": 1,
    "title": "John, Jane",
    "type": "DIRECT",
    "participants": [
      {
        "id": 1,
        "nickname": "John",
        "profileImageUrl": "http://localhost:8100/images/profile/1.jpg"
      },
      {
        "id": 2,
        "nickname": "Jane",
        "profileImageUrl": "http://localhost:8100/images/profile/2.jpg"
      }
    ],
    "lastMessage": null,
    "unreadCount": 0,
    "createdAt": "2023-06-15T10:30:00"
  }
}
```

#### Get User's Chat Rooms (사용자의 채팅방 목록 조회)
- **URL**: `/api/v1/chatrooms`
- **Method**: `GET`
- **Description**: Get all chat rooms for a user
- **설명**: 사용자의 모든 채팅방 목록을 조회합니다
- **Authentication**: Required (인증: 필수)
- **Request Parameters (요청 매개변수)**:
  - `userId` (long, required): User's ID (사용자 ID, 필수)
- **Example Request**:
```
curl -X GET "http://localhost:8100/api/v1/chatrooms?userId=1" \
  -H "Authorization: Bearer <your_jwt_token>"
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": null,
  "data": [
    {
      "id": 1,
      "title": "John, Jane",
      "type": "DIRECT",
      "participants": [
        {
          "id": 1,
          "nickname": "John",
          "profileImageUrl": "http://localhost:8100/images/profile/1.jpg"
        },
        {
          "id": 2,
          "nickname": "Jane",
          "profileImageUrl": "http://localhost:8100/images/profile/2.jpg"
        }
      ],
      "lastMessage": {
        "id": "msg123",
        "content": "Hello!",
        "senderId": 2,
        "senderNickname": "Jane",
        "timestamp": "2023-06-15T10:35:00"
      },
      "unreadCount": 1,
      "createdAt": "2023-06-15T10:30:00"
    }
  ]
}
```

#### Exit Chat Room (채팅방 나가기)
- **URL**: `/api/v1/chatrooms/{roomId}/exit`
- **Method**: `DELETE`
- **Description**: Leave a chat room
- **설명**: 채팅방을 나갑니다
- **Authentication**: Required (인증: 필수)
- **Path Parameters (경로 매개변수)**:
  - `roomId` (long, required): Chat room ID (채팅방 ID, 필수)
- **Request Parameters (요청 매개변수)**:
  - `userId` (long, required): User's ID (사용자 ID, 필수)
- **Example Request**:
```
curl -X DELETE "http://localhost:8100/api/v1/chatrooms/1/exit?userId=1" \
  -H "Authorization: Bearer <your_jwt_token>"
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": "채팅방에서 퇴장했습니다.",
  "data": true
}
```

#### Update Chat Room Title (채팅방 제목 변경)
- **URL**: `/api/v1/chatrooms/{roomId}/title`
- **Method**: `PUT`
- **Content-Type**: `application/json`
- **Description**: Update a chat room's title
- **설명**: 채팅방의 제목을 변경합니다
- **Authentication**: Required (인증: 필수)
- **Path Parameters (경로 매개변수)**:
  - `roomId` (long, required): Chat room ID (채팅방 ID, 필수)
- **Request Body**:
```json
{
  "title": "New Chat Room Title"
}
```
- **Example Request**:
```
curl -X PUT "http://localhost:8100/api/v1/chatrooms/1/title" \
  -H "Authorization: Bearer <your_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"New Chat Room Title"}'
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": "채팅방 제목이 변경되었습니다.",
  "data": true
}
```

### Messages (메시지)

#### Edit Message (메시지 편집)
- **URL**: `/api/v1/messages/edit`
- **Method**: `PUT`
- **Content-Type**: `application/json`
- **Description**: Edit a message's content
- **설명**: 메시지 내용을 편집합니다
- **Authentication**: Required (인증: 필수)
- **Request Body**:
```json
{
  "messageId": "msg123",
  "newContent": "Updated message content"
}
```
- **Example Request**:
```
curl -X PUT http://localhost:8100/api/v1/messages/edit \
  -H "Authorization: Bearer <your_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"messageId":"msg123","newContent":"Updated message content"}'
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": "메시지가 수정되었습니다.",
  "data": {
    "id": "msg123",
    "content": "Updated message content",
    "senderId": 1,
    "senderNickname": "John",
    "roomId": 1,
    "timestamp": "2023-06-15T10:40:00",
    "status": "EDITED",
    "reactions": []
  }
}
```

#### Delete Message (메시지 삭제)
- **URL**: `/api/v1/messages/delete`
- **Method**: `DELETE`
- **Content-Type**: `application/json`
- **Description**: Delete a message
- **설명**: 메시지를 삭제합니다
- **Authentication**: Required (인증: 필수)
- **Request Body**:
```json
{
  "messageId": "msg123"
}
```
- **Example Request**:
```
curl -X DELETE http://localhost:8100/api/v1/messages/delete \
  -H "Authorization: Bearer <your_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{"messageId":"msg123"}'
```
- **Response**:
```json
{
  "status": "SUCCESS",
  "message": "메시지가 삭제되었습니다.",
  "data": {
    "id": "msg123",
    "content": "This message has been deleted",
    "senderId": 1,
    "senderNickname": "John",
    "roomId": 1,
    "timestamp": "2023-06-15T10:40:00",
    "status": "DELETED",
    "reactions": []
  }
}
```

## WebSocket API (웹소켓 API)

### Connection (연결)

To connect to the WebSocket server:
웹소켓 서버에 연결하려면:

```
ws://localhost:8100/ws/chat
```

Or with SockJS:
또는 SockJS를 사용하여:

```
http://localhost:8100/ws/chat
```

Include your JWT token as a query parameter:
JWT 토큰을 쿼리 매개변수로 포함하세요:

```
ws://localhost:8100/ws/chat?token=<your_jwt_token>
```

### Destinations (목적지)

After connecting to the WebSocket, you can subscribe to the following destinations:
웹소켓에 연결한 후, 다음 목적지를 구독할 수 있습니다:

#### User-specific destinations (사용자별 목적지):
- `/user/queue/errors` - Receive error messages (오류 메시지 수신)
- `/user/queue/messages` - Receive messages sent to you (사용자에게 전송된 메시지 수신)

#### Chat room destinations (채팅방 목적지):
- `/topic/chat/{roomId}` - Receive messages in a specific chat room (특정 채팅방의 메시지 수신)

### Sending Messages (메시지 전송)

To send messages, use the following destinations:
메시지를 전송하려면 다음 목적지를 사용하세요:

#### Send Chat Message (채팅 메시지 전송)
- **Destination (목적지)**: `/app/chat`
- **Payload**:
```json
{
  "roomId": 1,
  "senderId": 1,
  "content": "Hello, this is a test message!",
  "type": "TEXT"
}
```

#### Sync Messages (Pagination) (메시지 동기화 - 페이지네이션)
- **Destination (목적지)**: `/app/sync`
- **Payload (페이로드)**:
```json
{
  "roomId": 1,
  "userId": 1,
  "direction": "INITIAL",
  "messageId": null,
  "limit": 20
}
```
- **Direction Options (방향 옵션)**:
  - `INITIAL` - Get initial messages (초기 메시지 가져오기)
  - `BEFORE` - Get messages before the specified messageId (지정된 메시지 ID 이전의 메시지 가져오기)
  - `AFTER` - Get messages after the specified messageId (지정된 메시지 ID 이후의 메시지 가져오기)

#### Mark Messages as Read (메시지 읽음 표시)
- **Destination (목적지)**: `/app/read`
- **Payload (페이로드)**:
```json
{
  "roomId": 1,
  "userId": 1,
  "messageId": "msg123"
}
```

#### Mark All Messages as Read (모든 메시지 읽음 표시)
- **Destination (목적지)**: `/app/read-all`
- **Payload (페이로드)**:
```json
{
  "roomId": 1,
  "userId": 1
}
```

#### Send Typing Indicator (타이핑 표시 전송)
- **Destination (목적지)**: `/app/typing`
- **Payload (페이로드)**:
```json
{
  "roomId": 1,
  "userId": 1,
  "isTyping": true
}
```

#### Thread Messages (스레드 메시지)

##### Get Thread Details (스레드 상세 정보 조회)
- **Destination (목적지)**: `/app/thread/detail`
- **Payload**:
```json
{
  "parentMessageId": "msg123",
  "userId": 1
}
```

##### Get Thread Messages (스레드 메시지 조회)
- **Destination (목적지)**: `/app/thread/messages`
- **Payload (페이로드)**:
```json
{
  "parentMessageId": "msg123",
  "userId": 1
}
```

##### Send Thread Message (스레드 메시지 전송)
- **Destination (목적지)**: `/app/thread`
- **Payload (페이로드)**:
```json
{
  "parentMessageId": "msg123",
  "roomId": 1,
  "senderId": 1,
  "content": "This is a reply in a thread",
  "type": "TEXT"
}
```

## Response Format (응답 형식)

All REST API responses follow this format:
모든 REST API 응답은 다음 형식을 따릅니다:

```json
{
  "status": "SUCCESS",
  "message": "Optional success message",
  "data": {
    "property1": "value1",
    "property2": "value2"
  }
}
```

Error responses (오류 응답):

```json
{
  "status": "ERROR",
  "message": "Error message",
  "data": null
}
```

## Error Codes (오류 코드)

The API may return various error codes. Common HTTP status codes include:
API는 다양한 오류 코드를 반환할 수 있습니다. 일반적인 HTTP 상태 코드는 다음과 같습니다:

- 200: OK (성공)
- 201: Created (생성됨)
- 400: Bad Request (잘못된 요청)
- 401: Unauthorized (인증되지 않음)
- 403: Forbidden (접근 금지)
- 404: Not Found (찾을 수 없음)
- 500: Internal Server Error (서버 내부 오류)

## Recommendations (권장 사항)

1. Always authenticate before making API calls (API 호출 전 항상 인증을 수행하세요)
2. Handle WebSocket reconnection in case of disconnection (연결 끊김 시 WebSocket 재연결을 처리하세요)
3. Implement proper error handling for both REST and WebSocket APIs (REST 및 WebSocket API 모두에 대해 적절한 오류 처리를 구현하세요)
4. For real-time features, prefer WebSocket over REST API when possible (실시간 기능의 경우 가능하면 REST API보다 WebSocket을 선호하세요)
5. Use pagination for retrieving large amounts of data (대량의 데이터를 검색할 때는 페이지네이션을 사용하세요)
