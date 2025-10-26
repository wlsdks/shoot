# Slack 모니터링 알림 설정 가이드

CDC 및 Outbox 이벤트 장애를 Slack으로 실시간 알림받는 방법입니다.

## 📋 개요

**구현된 Slack 알림:**
- DLQ 이벤트 발생 시 즉시 알림
- 미해결 DLQ 정기 알림 (매 시간)
- CDC 커넥터 장애 알림 (매 5분)
- Replication Lag 임계값 초과 알림

**현재 상태:**
- ✅ Slack 알림 코드: 구현 완료
- ⚠️ Slack 연동: 비활성화 (`slack.notification.enabled=false`)
- ℹ️ NoOp Adapter 활성화 중 (로그만 출력)

---

## 🚀 Step 1: Slack Incoming Webhook 생성

### 1-1. Slack Workspace 접속
- Slack 워크스페이스에 로그인합니다.

### 1-2. Incoming Webhook 앱 추가
1. Slack Workspace 설정 → **App Directory** 접속
2. **Incoming WebHooks** 검색 및 설치
3. 알림받을 채널 선택 (예: `#alerts`, `#shoot-monitoring`)
4. **Webhook URL** 복사
   ```
   https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX
   ```

---

## 🔧 Step 2: 환경 변수 설정

### 2-1. 로컬 개발 환경

**방법 1: 환경 변수로 설정 (권장)**
```bash
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
```

**방법 2: application.yml 직접 수정 (비권장 - 절대 커밋 금지)**
```yaml
slack:
  notification:
    enabled: true
    webhook-url: "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
    channel: "#alerts"
    username: "Shoot Alert Bot"
```

### 2-2. 프로덕션 환경

**Docker Compose:**
```yaml
environment:
  - SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
```

**Kubernetes Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: slack-webhook
type: Opaque
stringData:
  webhook-url: "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
```

```yaml
env:
  - name: SLACK_WEBHOOK_URL
    valueFrom:
      secretKeyRef:
        name: slack-webhook
        key: webhook-url
```

---

## ✅ Step 3: Slack 알림 활성화

### 3-1. application.yml 수정

```yaml
slack:
  notification:
    enabled: true  # false → true로 변경
    webhook-url: "${SLACK_WEBHOOK_URL:}"
    channel: "#alerts"  # 원하는 채널로 변경
    username: "Shoot Alert Bot"
```

### 3-2. 애플리케이션 재시작

```bash
./gradlew bootRun
```

### 3-3. 로그 확인

**Slack 활성화 성공 시:**
```log
SlackWebhookAdapter : Initializing Slack notification adapter
SlackWebhookAdapter : Slack webhook URL configured
```

**Slack 비활성화 시:**
```log
NoOpSlackNotificationAdapter : No-Op Slack adapter initialized
```

---

## 🧪 Step 4: 테스트

### 4-1. DLQ 이벤트 강제 발생

**잘못된 이벤트 삽입:**
```sql
INSERT INTO outbox_events (
    saga_id, saga_state, event_type, payload,
    processed, retry_count, created_at
) VALUES (
    'test-dlq-slack', 'STARTED',
    'com.stark.shoot.domain.event.InvalidEvent',  -- 존재하지 않는 클래스
    '{"invalid":"json"}',
    false, 6,  -- retry_count > MAX_RETRY_COUNT (5)
    NOW()
);
```

**예상 Slack 알림:**
```
🚨 DLQ 이벤트 발생

• Saga ID: `test-dlq-slack`
• 이벤트 타입: `com.stark.shoot.domain.event.InvalidEvent`
• 실패 원인: 재시도 횟수 초과 (6회)
• 시간: 2025-10-26T12:00:00Z

⚠️ 관리자 확인이 필요합니다.
```

### 4-2. CDC 커넥터 중지 테스트

**Debezium 커넥터 일시 중지:**
```bash
curl -X PUT http://localhost:8083/connectors/shoot-outbox-connector/pause
```

**5분 후 예상 Slack 알림:**
```
🔥 CDC 커넥터 장애

커넥터: shoot-outbox-connector
상태: PAUSED
태스크: task-0: PAUSED

• 시간: 2025-10-26T12:05:00Z
```

**커넥터 재개:**
```bash
curl -X PUT http://localhost:8083/connectors/shoot-outbox-connector/resume
```

### 4-3. Replication Lag 테스트

**대량 이벤트 삽입:**
```sql
INSERT INTO outbox_events (saga_id, saga_state, event_type, payload, processed, created_at)
SELECT
    'test-lag-' || generate_series(1, 1000),
    'STARTED',
    'com.stark.shoot.domain.event.message.MessageCreatedEvent',
    '{"messageId":"test"}',
    false,
    NOW();
```

**Lag 확인:**
```bash
curl http://localhost:8100/api/admin/cdc/replication/lag
```

---

## 📊 알림 종류 및 주기

| 알림 타입 | 트리거 조건 | 주기/지연 | 심각도 |
|----------|-----------|---------|--------|
| **DLQ 이벤트** | 재시도 5회 초과 | 즉시 | 🚨 HIGH |
| **미해결 DLQ** | DLQ 개수 > 0 | 매 시간 | ⚠️ MEDIUM |
| **CDC 커넥터 장애** | state != RUNNING | 매 5분 | 🔥 HIGH |
| **Replication Lag** | lag > 10 MB | 매 5분 | ⚠️ MEDIUM |

---

## 🔍 모니터링 API

### CDC 헬스 상태 전체 조회
```bash
curl http://localhost:8100/api/admin/cdc/health
```

**응답 예시:**
```json
{
  "healthy": true,
  "connector": {
    "name": "shoot-outbox-connector",
    "state": "RUNNING",
    "tasks": [
      {"id": 0, "state": "RUNNING", "workerId": "172.22.0.3:8083"}
    ]
  },
  "replication": {
    "slotName": "shoot_outbox_slot",
    "active": true,
    "lagBytes": 29600,
    "lagMb": 0
  },
  "checkedAt": "2025-10-26T12:00:00Z"
}
```

### Connector 상태만 조회
```bash
curl http://localhost:8100/api/admin/cdc/connector/status
```

### Replication Lag만 조회
```bash
curl http://localhost:8100/api/admin/cdc/replication/lag
```

---

## 🛠️ 트러블슈팅

### ❌ Slack 알림이 오지 않아요

**1. Webhook URL 확인**
```bash
# 환경 변수 확인
echo $SLACK_WEBHOOK_URL

# application.yml 확인
grep webhook-url src/main/resources/application.yml
```

**2. Slack enabled 확인**
```bash
grep "enabled:" src/main/resources/application.yml
# 결과: enabled: true 여야 함
```

**3. 로그 확인**
```bash
tail -f app.log | grep -i "slack"
```

**4. 수동 테스트**
```bash
curl -X POST ${SLACK_WEBHOOK_URL} \
  -H 'Content-Type: application/json' \
  -d '{
    "text": "Slack Webhook 테스트",
    "channel": "#alerts",
    "username": "Test Bot",
    "icon_emoji": ":robot_face:"
  }'
```

### ❌ "Slack Webhook URL이 설정되지 않았습니다" 로그

**원인:** `SLACK_WEBHOOK_URL` 환경 변수가 설정되지 않음

**해결:**
```bash
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."
./gradlew bootRun
```

### ❌ "HTTP 404 Not Found" 에러

**원인:** Webhook URL이 잘못되었거나 만료됨

**해결:** Slack에서 Incoming Webhook을 재생성하고 URL을 업데이트하세요.

### ❌ NoOpSlackNotificationAdapter가 계속 활성화됨

**원인:** `slack.notification.enabled=false` 또는 설정 안 함

**해결:**
1. application.yml에서 `enabled: true` 확인
2. 애플리케이션 재시작
3. 로그에서 `SlackWebhookAdapter` 확인

---

## 🔐 보안 권장사항

### ✅ DO
- ✅ 환경 변수로 Webhook URL 관리
- ✅ Kubernetes Secret/AWS Secrets Manager 사용
- ✅ Webhook URL을 `.gitignore`에 추가
- ✅ 팀 공유 채널 사용 (`#alerts`, `#monitoring`)

### ❌ DON'T
- ❌ Webhook URL을 코드에 직접 작성
- ❌ Webhook URL을 Git에 커밋
- ❌ Public 저장소에 Webhook URL 노출
- ❌ 개인 DM으로 알림 설정

---

## 📈 성능 영향

- **Slack API 호출:** 비동기 처리 (blocking 없음)
- **트랜잭션 커밋 후 전송:** 롤백 시 알림 안 감
- **실패 시:** 로그만 남기고 애플리케이션은 계속 동작
- **추가 지연:** ~50-200ms (네트워크 상황에 따라)

---

## 🎯 프로덕션 체크리스트

- [ ] Slack Incoming Webhook 생성
- [ ] Webhook URL을 Kubernetes Secret/환경 변수로 등록
- [ ] `slack.notification.enabled=true` 설정
- [ ] 알림 채널 설정 (`channel: "#production-alerts"`)
- [ ] 테스트 DLQ 이벤트로 알림 확인
- [ ] CDC 커넥터 장애 시뮬레이션 테스트
- [ ] 운영팀에게 알림 채널 공유
- [ ] On-call 로테이션 설정

---

**설정 완료 후 예상 알림 예시:**

```
🚨 DLQ 이벤트 발생

• Saga ID: `message-create-saga-12345`
• 이벤트 타입: `MessageCreatedEvent`
• 실패 원인: Kafka 전송 실패 (Connection timeout)
• 시간: 2025-10-26T12:34:56Z

⚠️ 관리자 확인이 필요합니다.
```

```
⚠️ 미해결 DLQ 알림

• 미해결 개수: *15*개

*최근 DLQ:*
```
id=42, sagaId=msg-saga-001, eventType=MessageCreatedEvent, reason=Deserialization failed
id=43, sagaId=msg-saga-002, eventType=MessageUpdatedEvent, reason=Connection timeout
...
```

👉 확인: /api/admin/outbox-dlq
```

---

*마지막 업데이트: 2025-10-26*
