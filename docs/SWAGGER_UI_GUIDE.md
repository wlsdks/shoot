# Swagger UI 사용 가이드

Shoot API 문서를 Swagger UI를 통해 확인하고 테스트하는 방법을 안내합니다.

## 목차

- [접속 방법](#접속-방법)
- [서비스별 API 그룹](#서비스별-api-그룹)
- [인증 설정](#인증-설정)
- [API 테스트 방법](#api-테스트-방법)
- [주요 기능](#주요-기능)
- [문제 해결](#문제-해결)

---

## 접속 방법

### 1. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 2. Swagger UI 접속

브라우저에서 다음 URL로 접속:

```
http://localhost:8080/swagger-ui.html
```

### 3. OpenAPI JSON 스펙 확인

Raw OpenAPI JSON 문서는 다음 경로에서 확인 가능:

```
http://localhost:8080/v3/api-docs
```

---

## 서비스별 API 그룹

Swagger UI 상단의 드롭다운 메뉴에서 서비스를 선택할 수 있습니다.

### 1. User Service

**엔드포인트**: 10개

**주요 기능**:
- 회원가입 / 로그인 / 로그아웃
- JWT 토큰 발급 및 갱신
- 사용자 프로필 조회 및 수정
- 사용자 검색

**예시 API**:
- `POST /api/v1/auth/register` - 회원가입
- `POST /api/v1/auth/login` - 로그인
- `GET /api/v1/users/me` - 내 정보 조회
- `PUT /api/v1/users/me` - 프로필 업데이트

---

### 2. Friend Service

**엔드포인트**: 15개

**주요 기능**:
- 친구 목록 조회 및 관리
- 친구 요청 전송 / 수락 / 거절
- 공통 친구 조회
- BFS 알고리즘 기반 친구 추천

**예시 API**:
- `POST /api/v1/friends/requests` - 친구 요청 전송
- `GET /api/v1/friends` - 친구 목록 조회
- `PUT /api/v1/friends/requests/{requestId}/accept` - 요청 수락
- `GET /api/v1/friends/recommendations` - 친구 추천

---

### 3. Chat Service

**엔드포인트**: 30개

**주요 기능**:
- 채팅방 생성 / 조회 / 삭제
- 참여자 관리
- 메시지 전송 / 수정 / 삭제
- 리액션 (LIKE, LOVE, HAHA 등)
- 스케줄 메시지
- 메시지 고정
- 메시지 전달 및 읽음 처리

**예시 API**:
- `POST /api/v1/chatrooms` - 채팅방 생성
- `POST /api/v1/chatrooms/{roomId}/messages` - 메시지 전송
- `POST /api/v1/chatrooms/{roomId}/messages/{messageId}/reactions` - 리액션 추가
- `GET /api/v1/chatrooms/{roomId}/messages` - 메시지 목록 조회

---

### 4. Notification Service

**엔드포인트**: 11개

**주요 기능**:
- 알림 목록 조회 및 상세 조회
- 읽음 처리 (개별/전체)
- 알림 삭제
- 알림 설정 관리

**예시 API**:
- `GET /api/v1/notifications` - 알림 목록 조회
- `PUT /api/v1/notifications/{notificationId}/read` - 읽음 처리
- `GET /api/v1/notifications/settings` - 알림 설정 조회
- `PUT /api/v1/notifications/settings` - 알림 설정 업데이트

---

### 5. All APIs

전체 66개의 엔드포인트를 한눈에 볼 수 있는 통합 뷰입니다.

---

### 6. Internal APIs

헬스체크, 모니터링 등 내부 API를 확인할 수 있습니다.

---

## 인증 설정

대부분의 API는 JWT 인증이 필요합니다. (회원가입/로그인 제외)

### 1. 토큰 발급

먼저 로그인 API를 통해 JWT 토큰을 발급받습니다.

#### 회원가입 (선택사항)

```
POST /api/v1/auth/register
```

**Request Body**:
```json
{
  "username": "testuser",
  "password": "password123",
  "nickname": "테스트유저"
}
```

#### 로그인

```
POST /api/v1/auth/login
```

**Request Body**:
```json
{
  "username": "testuser",
  "password": "password123"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  }
}
```

### 2. Swagger UI에서 인증 설정

1. Swagger UI 우측 상단의 **"Authorize"** 버튼 클릭
2. "bearerAuth" 섹션에 발급받은 `accessToken` 값 입력
   - ⚠️ **주의**: `Bearer ` 접두사는 자동으로 추가되므로 토큰 값만 입력
   - 예시: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. **"Authorize"** 버튼 클릭
4. **"Close"** 버튼으로 모달 닫기

이제 모든 API 요청에 자동으로 JWT 토큰이 포함됩니다.

### 3. 토큰 만료 시

토큰이 만료되면 Refresh Token API를 사용하여 새 토큰을 발급받습니다.

```
POST /api/v1/auth/refresh
```

**Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## API 테스트 방법

### 1. API 선택

서비스 그룹을 선택한 후, 테스트할 API를 클릭합니다.

### 2. Try it out

API 상세 페이지에서 **"Try it out"** 버튼을 클릭합니다.

### 3. 파라미터 입력

- **Path Parameters**: URL 경로에 포함되는 파라미터 (예: `{roomId}`, `{userId}`)
- **Query Parameters**: URL 쿼리 스트링 (예: `?page=0&size=20`)
- **Request Body**: POST/PUT 요청의 본문 (JSON 형식)

### 4. Execute

**"Execute"** 버튼을 클릭하여 API를 호출합니다.

### 5. 응답 확인

- **Response Body**: API 응답 본문 (JSON)
- **Response Headers**: 응답 헤더
- **Response Status**: HTTP 상태 코드
- **Curl**: 동일한 요청을 curl 명령어로 실행하는 방법
- **Request URL**: 실제 호출된 URL

---

## 주요 기능

### 1. 실시간 API 테스트

Swagger UI에서 바로 API를 호출하고 결과를 확인할 수 있습니다.

### 2. 요청/응답 예시

각 API의 요청 파라미터와 응답 스키마를 확인할 수 있습니다.

### 3. 모델 스키마

"Schemas" 섹션에서 모든 데이터 모델의 구조를 확인할 수 있습니다.

### 4. 필터링

Swagger UI 상단의 필터 입력창을 사용하여 특정 API를 빠르게 찾을 수 있습니다.

### 5. API 복사

각 API의 curl 명령어를 복사하여 터미널에서 실행할 수 있습니다.

### 6. 문법 강조

JSON 응답이 Monokai 테마로 강조 표시되어 가독성이 향상됩니다.

---

## 예제 워크플로우

### 시나리오: 친구와 1:1 채팅 시작하기

#### 1. 회원가입 및 로그인

```
POST /api/v1/auth/register
POST /api/v1/auth/login
```

로그인 후 받은 `accessToken`을 Swagger UI에 설정합니다.

#### 2. 친구 추가

```
POST /api/v1/friends/requests
{
  "friendId": 2
}
```

친구 요청을 보냅니다.

#### 3. 친구 요청 수락 (상대방)

```
PUT /api/v1/friends/requests/{requestId}/accept
```

상대방이 요청을 수락합니다.

#### 4. 1:1 채팅방 생성

```
POST /api/v1/chatrooms
{
  "participantIds": [1, 2],
  "type": "DIRECT"
}
```

#### 5. 메시지 전송

```
POST /api/v1/chatrooms/{roomId}/messages
{
  "content": "안녕하세요!",
  "type": "TEXT"
}
```

#### 6. 메시지 목록 조회

```
GET /api/v1/chatrooms/{roomId}/messages?page=0&size=20
```

---

## OpenAPI 스펙 파일

각 서비스의 OpenAPI 3.0 스펙은 YAML 형식으로 제공됩니다:

- **User Service**: `docs/api/user-service-api.yaml`
- **Friend Service**: `docs/api/friend-service-api.yaml`
- **Chat Service**: `docs/api/chat-service-api.yaml`
- **Notification Service**: `docs/api/notification-service-api.yaml`

### 스펙 파일 활용

이 파일들은 다음 용도로 사용할 수 있습니다:

1. **코드 생성**: OpenAPI Generator를 사용한 클라이언트 SDK 생성
2. **API 모킹**: Prism, Mockoon 등을 사용한 Mock API 서버 구축
3. **API 테스팅**: Postman, Insomnia로 컬렉션 임포트
4. **문서화**: Redoc, Stoplight 등 다른 문서화 도구 사용

---

## 문제 해결

### Swagger UI가 로드되지 않음

**원인**: 애플리케이션이 실행되지 않았거나 포트가 다름

**해결**:
```bash
# 애플리케이션 실행 확인
./gradlew bootRun

# 포트 확인
curl http://localhost:8080/v3/api-docs
```

---

### 401 Unauthorized 오류

**원인**: JWT 토큰이 설정되지 않았거나 만료됨

**해결**:
1. `/api/v1/auth/login`으로 새 토큰 발급
2. Swagger UI의 "Authorize" 버튼으로 토큰 설정
3. 토큰 만료 시 `/api/v1/auth/refresh`로 갱신

---

### 403 Forbidden 오류

**원인**: 토큰은 유효하지만 해당 리소스에 대한 권한이 없음

**해결**:
- 자신의 리소스만 접근 가능한지 확인 (예: 다른 사용자의 프로필 수정 불가)
- Admin 권한이 필요한 API인지 확인

---

### CORS 오류

**원인**: 다른 도메인에서 API 호출 시 CORS 정책 위반

**해결**:
- Swagger UI는 같은 오리진이므로 CORS 문제가 없음
- 외부 클라이언트는 `application.yml`의 CORS 설정 확인:
  ```yaml
  spring:
    web:
      cors:
        allowed-origins: http://localhost:3000
  ```

---

### API 응답이 느림

**원인**: 대량의 데이터 조회 또는 복잡한 쿼리

**해결**:
- 페이지네이션 파라미터 사용: `?page=0&size=20`
- 필요한 필드만 요청하는 API 사용
- 인덱스가 적용된 필드로 검색

---

## 개발 팁

### 1. curl 명령어 복사

각 API 실행 후 "Curl" 탭에서 명령어를 복사하여 터미널에서 재사용할 수 있습니다.

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/chatrooms' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "participantIds": [1, 2],
  "type": "DIRECT"
}'
```

### 2. Postman 연동

OpenAPI JSON을 Postman으로 임포트하여 컬렉션을 자동 생성할 수 있습니다.

1. Postman 실행
2. Import > Link 선택
3. URL 입력: `http://localhost:8080/v3/api-docs`
4. Import 클릭

### 3. 자동화 스크립트

Swagger UI의 응답을 참고하여 자동화 테스트 스크립트를 작성할 수 있습니다.

```bash
#!/bin/bash

# 로그인
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' \
  | jq -r '.data.accessToken')

# API 호출
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## 관련 문서

- [API 문서 인덱스](./api/README.md) - 전체 API 개요
- [Event Schema](./events/EVENT_SCHEMA.md) - 이벤트 스키마 정의
- [CLAUDE.md](../CLAUDE.md) - 프로젝트 아키텍처
- [DOMAIN.md](../DOMAIN.md) - 도메인 모델 상세

---

## 지원

문제가 발생하거나 질문이 있으면 다음을 확인하세요:

1. **로그 확인**: `logs/application.log`
2. **API 문서**: `docs/api/README.md`
3. **이슈 트래커**: GitHub Issues

---

**Last updated**: 2025-11-09
**Swagger UI Version**: SpringDoc OpenAPI 2.8.9
**API Version**: v1
