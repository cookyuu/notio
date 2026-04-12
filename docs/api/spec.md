# Notio API 명세서

> **버전**: v1.2
> **Base URL**: `http://localhost:8080` (로컬 개발)
> **API 버전**: `/api/v1`
> **최종 수정**: 2026-04-10

---

## ⚠️ 중요 사항

### 이 명세서의 목적

이 API 명세서는 **Backend와 Frontend 개발의 계약(Contract)** 입니다.

**Backend 개발자:**
- 이 명세에 정의된 엔드포인트, 필드명, 타입, 제약사항을 **100% 준수**하여 구현해야 합니다.
- 각 필드의 `필수/선택`, `Nullable 여부`, `길이 제한`을 반드시 검증해야 합니다.
- 명세에 정의된 HTTP 상태 코드와 에러 코드를 사용해야 합니다.

**Frontend 개발자:**
- 이 명세를 기준으로 Request DTO와 Response DTO를 정의할 수 있습니다.
- 각 필드의 Nullable 여부를 확인하여 타입을 정의해야 합니다.
- Validation 규칙을 참고하여 클라이언트 측 검증을 구현할 수 있습니다.

**중요:** API 변경이 필요한 경우:
1. 이 명세서를 먼저 수정
2. Backend/Frontend 팀이 변경사항 리뷰
3. 합의 후 구현 시작

---

## 목차

1. [공통 사항](#1-공통-사항)
2. [인증](#2-인증)
3. [Notification API](#3-notification-api)
4. [Webhook API](#4-webhook-api)
5. [Chat API](#5-chat-api)
6. [Todo API](#6-todo-api)
7. [Push API](#7-push-api)
8. [Analytics API](#8-analytics-api)
9. [에러 코드](#9-에러-코드)

---

## 1. 공통 사항

### 1.1 공통 응답 형식

모든 API는 다음 형식의 응답을 반환합니다:

**성공 응답:**
```json
{
  "success": true,
  "data": { /* 실제 데이터 */ },
  "error": null
}
```

**에러 응답:**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

### 1.2 HTTP 상태 코드

| 상태 코드 | 설명 |
|----------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 204 | 성공 (응답 본문 없음) |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 404 | 리소스 없음 |
| 500 | 서버 내부 오류 |
| 503 | 서비스 사용 불가 (LLM 장애 등) |

### 1.3 페이지네이션

목록 조회 API는 다음 쿼리 파라미터를 지원합니다:

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | int | 0 | 페이지 번호 (0부터 시작) |
| `size` | int | 20 | 페이지 크기 |

**페이지네이션 응답:**
```json
{
  "success": true,
  "data": {
    "content": [ /* 항목 배열 */ ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "first": true,
    "last": false
  },
  "error": null
}
```

### 1.4 공통 타입 정의

**NotificationSource (enum)**
```
CLAUDE, SLACK, GITHUB, GMAIL, INTERNAL
```

**NotificationPriority (enum)**
```
URGENT, HIGH, MEDIUM, LOW
```

**TodoStatus (enum)**
```
PENDING, IN_PROGRESS, DONE
```

**MessageRole (enum)**
```
USER, ASSISTANT
```

### 1.5 필드 명세 규칙

이 문서에서 사용하는 필드 표기법:

| 표기 | 의미 |
|------|------|
| `*` | 필수 (Required) |
| - | 선택 (Optional) |
| `nullable` | null 값 허용 |
| `min/max` | 최소/최대 길이 또는 값 |
| `default` | 기본값 |

---

## 2. 인증

### 2.1 개요

Phase 0 (MVP)에서는 인증이 구현되지 않았습니다.
Phase 2+ 에서 JWT 기반 인증이 추가될 예정입니다.

**Phase 2+ 인증 헤더 (예정):**
```
Authorization: Bearer {JWT_TOKEN}
```

---

## 3. Notification API

### 3.1 알림 목록 조회

**Endpoint:** `GET /api/v1/notifications`

**설명:** 알림 목록을 조회합니다. 필터링 및 페이지네이션을 지원합니다.

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 기본값 | 설명 |
|---------|------|------|----------|--------|------|
| `source` | string | - | enum: CLAUDE, SLACK, GITHUB, GMAIL, INTERNAL | - | 알림 소스 필터 |
| `is_read` | boolean | - | true/false | - | 읽음 상태 필터 |
| `page` | int | - | min: 0 | 0 | 페이지 번호 (0부터 시작) |
| `size` | int | - | min: 1, max: 100 | 20 | 페이지 크기 |

**Request Example:**
```http
GET /api/v1/notifications?source=SLACK&is_read=false&page=0&size=20
```

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `content` | array | N | 알림 객체 배열 |
| `page` | int | N | 현재 페이지 번호 |
| `size` | int | N | 페이지 크기 |
| `totalElements` | int | N | 전체 항목 수 |
| `totalPages` | int | N | 전체 페이지 수 |
| `first` | boolean | N | 첫 페이지 여부 |
| `last` | boolean | N | 마지막 페이지 여부 |

`content` 배열의 각 알림 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | - | 알림 ID |
| `source` | string | N | enum: CLAUDE, SLACK, GITHUB, GMAIL, INTERNAL | 알림 소스 |
| `title` | string | N | max: 255 | 알림 제목 |
| `body` | string | N | max: 2000 | 알림 본문 |
| `priority` | string | N | enum: URGENT, HIGH, MEDIUM, LOW | 우선순위 |
| `is_read` | boolean | N | - | 읽음 여부 |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |
| `updated_at` | string | N | ISO 8601 format | 수정 시각 |
| `external_id` | string | Y | max: 255 | 외부 서비스의 ID |
| `external_url` | string | Y | max: 500 | 외부 서비스 URL |
| `metadata` | object | Y | - | 추가 메타데이터 (JSON 객체) |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "source": "SLACK",
        "title": "#dev-team 채널에 멘션",
        "body": "@cookyuu PR 리뷰 부탁드립니다",
        "priority": "HIGH",
        "is_read": false,
        "created_at": "2026-04-10T10:30:00Z",
        "updated_at": "2026-04-10T10:30:00Z",
        "external_id": "slack-msg-20260410-001",
        "external_url": "https://notio-team.slack.com/archives/C123456/p1712421234",
        "metadata": {
          "channel": "dev-team",
          "user": "박철수",
          "user_id": "U123456"
        }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 68,
    "totalPages": 4,
    "first": true,
    "last": false
  },
  "error": null
}
```

### 3.2 알림 상세 조회

**Endpoint:** `GET /api/v1/notifications/{id}`

**설명:** 특정 알림의 상세 정보를 조회합니다. 조회 시 자동으로 읽음 처리됩니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | 알림 ID |

**Response 200 OK - 필드 명세:**

`data` 객체 (알림 객체와 동일):

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | - | 알림 ID |
| `source` | string | N | enum: CLAUDE, SLACK, GITHUB, GMAIL, INTERNAL | 알림 소스 |
| `title` | string | N | max: 255 | 알림 제목 |
| `body` | string | N | max: 2000 | 알림 본문 |
| `priority` | string | N | enum: URGENT, HIGH, MEDIUM, LOW | 우선순위 |
| `is_read` | boolean | N | - | 읽음 여부 (조회 시 자동으로 true 처리) |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |
| `updated_at` | string | N | ISO 8601 format | 수정 시각 (읽음 처리 시 갱신) |
| `external_id` | string | Y | max: 255 | 외부 서비스의 ID |
| `external_url` | string | Y | max: 500 | 외부 서비스 URL |
| `metadata` | object | Y | - | 추가 메타데이터 (JSON 객체) |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "source": "SLACK",
    "title": "#dev-team 채널에 멘션",
    "body": "@cookyuu PR 리뷰 부탁드립니다",
    "priority": "HIGH",
    "is_read": true,
    "created_at": "2026-04-10T10:30:00Z",
    "updated_at": "2026-04-10T10:35:00Z",
    "external_id": "slack-msg-20260410-001",
    "external_url": "https://notio-team.slack.com/archives/C123456/p1712421234",
    "metadata": {
      "channel": "dev-team",
      "user": "박철수",
      "user_id": "U123456"
    }
  },
  "error": null
}
```

**Error Cases:**
- `404 NOTIFICATION_NOT_FOUND`: 알림을 찾을 수 없음

### 3.3 알림 읽음 처리

**Endpoint:** `PATCH /api/v1/notifications/{id}/read`

**설명:** 특정 알림을 읽음 상태로 변경합니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | 알림 ID |

**Request Body:** 없음

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `id` | int | N | 알림 ID |
| `is_read` | boolean | N | 읽음 여부 (항상 true) |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "is_read": true
  },
  "error": null
}
```

**Error Cases:**
- `404 NOTIFICATION_NOT_FOUND`: 알림을 찾을 수 없음

### 3.4 전체 알림 읽음 처리

**Endpoint:** `PATCH /api/v1/notifications/read-all`

**설명:** 모든 미읽음 알림을 읽음 상태로 변경합니다.

**Request Body:** 없음

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `updated_count` | int | N | 읽음 처리된 알림 개수 |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "updated_count": 15
  },
  "error": null
}
```

### 3.5 미읽음 알림 수 조회

**Endpoint:** `GET /api/v1/notifications/unread-count`

**설명:** 미읽음 알림의 개수를 조회합니다. Redis 캐시를 사용하여 빠른 응답을 제공합니다.

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `count` | int | N | 미읽음 알림 개수 (min: 0) |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "count": 12
  },
  "error": null
}
```

### 3.6 알림 삭제

**Endpoint:** `DELETE /api/v1/notifications/{id}`

**설명:** 특정 알림을 삭제합니다 (Soft Delete - deleted_at 컬럼 설정).

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | 알림 ID |

**Request Body:** 없음

**Response 200 OK:**
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

**Error Cases:**
- `404 NOTIFICATION_NOT_FOUND`: 알림을 찾을 수 없음

---

## 4. Webhook API

### 4.1 Webhook 수신

**Endpoint:** `POST /api/v1/webhook/{source}`

**설명:** 외부 서비스로부터 Webhook을 수신합니다. 각 소스별로 서명 검증이 수행됩니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `source` | string | * | enum: claude, slack, github | 알림 소스 (소문자) |

**Required Headers (소스별):**

| 소스 | 헤더명 | 타입 | 필수 | 검증 방식 |
|------|--------|------|------|-----------|
| Claude | `Authorization` | string | * | Bearer 토큰 검증 (NOTIO_INTERNAL_TOKEN) |
| Slack | `X-Slack-Signature` | string | * | HMAC-SHA256 서명 검증 |
| Slack | `X-Slack-Request-Timestamp` | string | * | 타임스탬프 (Replay Attack 방지) |
| GitHub | `X-Hub-Signature-256` | string | * | HMAC-SHA256 서명 검증 |
| GitHub | `X-GitHub-Event` | string | * | 이벤트 타입 |

#### 4.1.1 Claude Code Webhook

**Request Body 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `event_type` | string | * | max: 100 | 이벤트 타입 (예: "notification") |
| `notification` | object | * | - | 알림 데이터 객체 |
| `notification.id` | string | * | max: 255 | Claude 서비스의 알림 ID |
| `notification.title` | string | * | max: 255 | 알림 제목 |
| `notification.message` | string | * | max: 2000 | 알림 메시지 |
| `notification.url` | string | - | max: 500 | 관련 URL |
| `notification.priority` | string | - | enum: urgent, high, medium, low | 우선순위 (기본값: medium) |
| `notification.timestamp` | string | * | ISO 8601 format | 이벤트 발생 시각 |

**Request Example:**
```json
{
  "event_type": "notification",
  "notification": {
    "id": "claude-notif-001",
    "title": "Claude Code 세션 종료",
    "message": "세션이 성공적으로 완료되었습니다",
    "url": "https://claude.ai/sessions/abc123",
    "priority": "medium",
    "timestamp": "2026-04-10T10:30:00Z"
  }
}
```

#### 4.1.2 Slack Webhook

**Request Body 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `type` | string | * | enum: event_callback, url_verification | Slack 이벤트 타입 |
| `event` | object | * | - | 이벤트 데이터 객체 |
| `event.type` | string | * | max: 100 | 이벤트 서브타입 (예: app_mention, message) |
| `event.user` | string | * | max: 50 | Slack 유저 ID |
| `event.text` | string | * | max: 2000 | 메시지 텍스트 |
| `event.channel` | string | * | max: 50 | 채널 ID |
| `event.ts` | string | * | max: 50 | 타임스탬프 (Slack 포맷) |
| `event.channel_type` | string | - | enum: channel, group, im, mpim | 채널 타입 |

**Request Example:**
```json
{
  "type": "event_callback",
  "event": {
    "type": "app_mention",
    "user": "U123456",
    "text": "@notio PR 리뷰 부탁드립니다",
    "channel": "C123456",
    "ts": "1712421234.123456",
    "channel_type": "channel"
  }
}
```

#### 4.1.3 GitHub Webhook

**Request Body 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `action` | string | * | max: 100 | 액션 타입 (예: opened, closed, reopened) |
| `pull_request` | object | * | - | PR 데이터 객체 (pull_request 이벤트) |
| `pull_request.id` | int | * | - | GitHub PR ID |
| `pull_request.number` | int | * | - | PR 번호 |
| `pull_request.title` | string | * | max: 255 | PR 제목 |
| `pull_request.html_url` | string | * | max: 500 | PR URL |
| `pull_request.user` | object | * | - | PR 생성자 정보 |
| `pull_request.user.login` | string | * | max: 100 | GitHub 유저명 |
| `issue` | object | - | - | 이슈 데이터 객체 (issues 이벤트) |
| `repository` | object | * | - | 리포지토리 정보 |

**Request Example (Pull Request):**
```json
{
  "action": "opened",
  "pull_request": {
    "id": 123,
    "number": 456,
    "title": "Add notification feature",
    "html_url": "https://github.com/user/repo/pull/456",
    "user": {
      "login": "cookyuu"
    }
  },
  "repository": {
    "name": "notio",
    "full_name": "user/notio"
  }
}
```

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `notification_id` | int | N | 생성된 알림 ID |
| `processed_at` | string | N | 처리 완료 시각 (ISO 8601) |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "notification_id": 42,
    "processed_at": "2026-04-10T10:30:05Z"
  },
  "error": null
}
```

**Error Cases:**
- `400 UNSUPPORTED_SOURCE`: 지원하지 않는 소스
- `400 INVALID_REQUEST`: 잘못된 요청 (필수 필드 누락)
- `401 WEBHOOK_VERIFICATION_FAILED`: 서명 검증 실패
- `500 INTERNAL_SERVER_ERROR`: 알림 생성 실패

---

## 5. Chat API

### 5.1 채팅 메시지 전송

**Endpoint:** `POST /api/v1/chat`

**설명:** AI에게 메시지를 전송하고 응답을 받습니다.

**Request Body 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `content` | string | * | min: 1, max: 1000 | 사용자 메시지 내용 |

**Request Example:**
```json
{
  "content": "오늘 받은 알림 중에 중요한 것들 알려줘"
}
```

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `message` | object | N | AI 응답 메시지 객체 |

`message` 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | - | 메시지 ID |
| `role` | string | N | enum: USER, ASSISTANT | 메시지 역할 |
| `content` | string | N | max: 4000 | 메시지 내용 |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "message": {
      "id": 123,
      "role": "ASSISTANT",
      "content": "오늘 받은 알림 중 중요한 것은 다음과 같습니다:\n\n1. Slack #dev-team 채널 멘션 - PR 리뷰 요청\n2. GitHub PR #456 승인 완료\n3. Claude Code 세션 완료\n\n총 3건의 중요 알림이 있습니다.",
      "created_at": "2026-04-10T10:35:00Z"
    }
  },
  "error": null
}
```

**Error Cases:**
- `400 INVALID_REQUEST`: 잘못된 요청 (content 누락 또는 길이 초과)
- `503 LLM_UNAVAILABLE`: LLM 서비스 사용 불가

### 5.2 채팅 스트리밍 (SSE)

**Endpoint:** `GET /api/v1/chat/stream`

**설명:** Server-Sent Events를 통해 AI 응답을 스트리밍으로 받습니다.

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `content` | string | * | min: 1, max: 1000 | 사용자 메시지 |

**Request Headers:**

| 헤더 | 타입 | 필수 | 값 |
|------|------|------|-----|
| `Accept` | string | * | `text/event-stream` |

**Request Example:**
```http
GET /api/v1/chat/stream?content=오늘%20받은%20알림%20요약해줘
Accept: text/event-stream
```

**Response (SSE Stream) - 필드 명세:**

각 청크(Chunk) 이벤트:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `chunk` | string | N | AI 응답의 일부 텍스트 조각 |

완료(Done) 이벤트:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `done` | boolean | N | 스트림 완료 여부 (항상 true) |
| `message_id` | int | N | 저장된 메시지 ID |

**Response Example:**
```
data: {"chunk": "오늘"}

data: {"chunk": " 받은"}

data: {"chunk": " 알림은"}

data: {"chunk": " 총"}

data: {"chunk": " 5건입니다."}

data: {"done": true, "message_id": 124}
```

**Error Cases:**
- `400 INVALID_REQUEST`: 잘못된 요청 (content 누락 또는 길이 초과)
- `503 LLM_UNAVAILABLE`: LLM 서비스 사용 불가

### 5.3 일일 요약 조회

**Endpoint:** `GET /api/v1/chat/daily-summary`

**설명:** 오늘 받은 알림의 AI 요약을 조회합니다. Redis에 24시간 캐시됩니다.

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `summary` | string | N | max: 2000 | 오늘의 알림 요약 |
| `date` | string | N | YYYY-MM-DD format | 요약 날짜 |
| `total_messages` | int | N | min: 0 | 오늘 받은 총 알림 개수 |
| `topics` | array | N | max items: 10 | 주요 주제 배열 (각 항목 max: 100) |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "summary": "오늘은 총 15건의 알림을 받았습니다. 주로 Slack에서 팀 멘션과 PR 리뷰 요청이 많았으며, GitHub에서는 2건의 PR이 승인되었습니다. 중요도가 높은 알림은 5건이며, 모두 확인하셨습니다.",
    "date": "2026-04-10",
    "total_messages": 15,
    "topics": [
      "PR 리뷰",
      "팀 멘션",
      "코드 승인",
      "배포 알림"
    ]
  },
  "error": null
}
```

**Error Cases:**
- `503 LLM_UNAVAILABLE`: LLM 서비스 사용 불가

### 5.4 채팅 히스토리 조회

**Endpoint:** `GET /api/v1/chat/history`

**설명:** 채팅 히스토리를 페이지네이션하여 조회합니다. 최신순으로 정렬됩니다.

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 기본값 | 설명 |
|---------|------|------|----------|--------|------|
| `page` | int | - | min: 0 | 0 | 페이지 번호 |
| `size` | int | - | min: 1, max: 100 | 20 | 페이지 크기 |

**Response 200 OK - 필드 명세:**

`data` 객체 (페이지네이션 응답과 동일):

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `content` | array | N | 메시지 객체 배열 |
| `page` | int | N | 현재 페이지 번호 |
| `size` | int | N | 페이지 크기 |
| `totalElements` | int | N | 전체 메시지 수 |
| `totalPages` | int | N | 전체 페이지 수 |
| `first` | boolean | N | 첫 페이지 여부 |
| `last` | boolean | N | 마지막 페이지 여부 |

`content` 배열의 각 메시지 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | - | 메시지 ID |
| `role` | string | N | enum: USER, ASSISTANT | 메시지 역할 |
| `content` | string | N | max: 4000 | 메시지 내용 |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 123,
        "role": "USER",
        "content": "오늘 받은 알림 요약해줘",
        "created_at": "2026-04-10T10:30:00Z"
      },
      {
        "id": 124,
        "role": "ASSISTANT",
        "content": "오늘은 총 15건의 알림을 받았습니다...",
        "created_at": "2026-04-10T10:30:05Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3,
    "first": true,
    "last": false
  },
  "error": null
}
```

---

## 6. Todo API

### 6.1 할일 생성

**Endpoint:** `POST /api/v1/todos`

**설명:** 새로운 할일을 생성합니다. `notification_id`를 제공하면 LLM이 알림 내용을 분석하여 자동으로 제목을 생성합니다.

**Request Body 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `notification_id` | int | - | min: 1 | 연관된 알림 ID (제공 시 title 자동 생성) |
| `title` | string | 조건부* | max: 255 | 할일 제목 (notification_id 미제공 시 필수) |
| `description` | string | - | max: 1000 | 할일 상세 설명 |
| `due_date` | string | - | ISO 8601 format | 마감 기한 |

**※ title 필수 조건:**
- `notification_id`가 **없으면** `title` 필수
- `notification_id`가 **있으면** `title` 선택 (LLM이 자동 생성)

**Request Example (알림에서 생성):**
```json
{
  "notification_id": 42,
  "description": "알림 기능 구현 PR 리뷰",
  "due_date": "2026-04-11T17:00:00Z"
}
```

**Request Example (직접 생성):**
```json
{
  "title": "PR #456 리뷰하기",
  "description": "알림 기능 구현 PR 리뷰",
  "due_date": "2026-04-11T17:00:00Z"
}
```

**Response 201 Created - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | - | 할일 ID |
| `notification_id` | int | Y | - | 연관된 알림 ID |
| `title` | string | N | max: 255 | 할일 제목 |
| `description` | string | Y | max: 1000 | 할일 상세 설명 |
| `status` | string | N | enum: PENDING, IN_PROGRESS, DONE | 상태 (초기값: PENDING) |
| `due_date` | string | Y | ISO 8601 format | 마감 기한 |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |
| `updated_at` | string | N | ISO 8601 format | 수정 시각 |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "id": 10,
    "notification_id": 42,
    "title": "PR #456 리뷰하기",
    "description": "알림 기능 구현 PR 리뷰",
    "status": "PENDING",
    "due_date": "2026-04-11T17:00:00Z",
    "created_at": "2026-04-10T10:40:00Z",
    "updated_at": "2026-04-10T10:40:00Z"
  },
  "error": null
}
```

**Error Cases:**
- `400 INVALID_REQUEST`: 잘못된 요청 (title, notification_id 모두 누락)
- `404 NOTIFICATION_NOT_FOUND`: 알림을 찾을 수 없음 (notification_id 제공 시)

### 6.2 할일 목록 조회

**Endpoint:** `GET /api/v1/todos`

**설명:** 할일 목록을 조회합니다. 상태별 필터링 및 페이지네이션을 지원합니다.

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 기본값 | 설명 |
|---------|------|------|----------|--------|------|
| `status` | string | - | enum: PENDING, IN_PROGRESS, DONE | - | 상태 필터 |
| `page` | int | - | min: 0 | 0 | 페이지 번호 |
| `size` | int | - | min: 1, max: 100 | 20 | 페이지 크기 |

**Response 200 OK - 필드 명세:**

`data` 객체 (페이지네이션 응답):

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `content` | array | N | 할일 객체 배열 |
| `page` | int | N | 현재 페이지 번호 |
| `size` | int | N | 페이지 크기 |
| `totalElements` | int | N | 전체 항목 수 |
| `totalPages` | int | N | 전체 페이지 수 |
| `first` | boolean | N | 첫 페이지 여부 |
| `last` | boolean | N | 마지막 페이지 여부 |

`content` 배열의 각 할일 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | - | 할일 ID |
| `notification_id` | int | Y | - | 연관된 알림 ID |
| `title` | string | N | max: 255 | 할일 제목 |
| `description` | string | Y | max: 1000 | 할일 상세 설명 |
| `status` | string | N | enum: PENDING, IN_PROGRESS, DONE | 상태 |
| `due_date` | string | Y | ISO 8601 format | 마감 기한 |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |
| `updated_at` | string | N | ISO 8601 format | 수정 시각 |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 10,
        "notification_id": 42,
        "title": "PR #456 리뷰하기",
        "description": "알림 기능 구현 PR 리뷰",
        "status": "PENDING",
        "due_date": "2026-04-11T17:00:00Z",
        "created_at": "2026-04-10T10:40:00Z",
        "updated_at": "2026-04-10T10:40:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 8,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "error": null
}
```

### 6.3 할일 상태 변경

**Endpoint:** `PATCH /api/v1/todos/{id}`

**설명:** 할일의 상태를 변경합니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | 할일 ID |

**Request Body 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `status` | string | * | enum: PENDING, IN_PROGRESS, DONE | 변경할 상태 |

**Request Example:**
```json
{
  "status": "IN_PROGRESS"
}
```

**Response 200 OK - 필드 명세:**

`data` 객체 (할일 객체와 동일):

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | - | 할일 ID |
| `notification_id` | int | Y | - | 연관된 알림 ID |
| `title` | string | N | max: 255 | 할일 제목 |
| `description` | string | Y | max: 1000 | 할일 상세 설명 |
| `status` | string | N | enum: PENDING, IN_PROGRESS, DONE | 상태 (변경됨) |
| `due_date` | string | Y | ISO 8601 format | 마감 기한 |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |
| `updated_at` | string | N | ISO 8601 format | 수정 시각 (갱신됨) |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "id": 10,
    "notification_id": 42,
    "title": "PR #456 리뷰하기",
    "description": "알림 기능 구현 PR 리뷰",
    "status": "IN_PROGRESS",
    "due_date": "2026-04-11T17:00:00Z",
    "created_at": "2026-04-10T10:40:00Z",
    "updated_at": "2026-04-10T11:00:00Z"
  },
  "error": null
}
```

**Error Cases:**
- `400 INVALID_REQUEST`: 잘못된 상태값
- `404 TODO_NOT_FOUND`: 할일을 찾을 수 없음

### 6.4 할일 삭제

**Endpoint:** `DELETE /api/v1/todos/{id}`

**설명:** 할일을 삭제합니다 (Soft Delete - deleted_at 컬럼 설정).

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | 할일 ID |

**Request Body:** 없음

**Response 200 OK:**
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

**Error Cases:**
- `404 TODO_NOT_FOUND`: 할일을 찾을 수 없음

---

## 7. Push API

### 7.1 디바이스 등록

**Endpoint:** `POST /api/v1/devices/register`

**설명:** FCM 토큰을 등록하여 푸시 알림을 받을 수 있도록 합니다. 동일한 device_id로 재등록 시 토큰이 업데이트됩니다.

**Request Body 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `device_id` | string | * | max: 255 | 기기 고유 ID (UUID 권장) |
| `fcm_token` | string | * | max: 500 | Firebase Cloud Messaging 토큰 |
| `platform` | string | * | enum: ANDROID, IOS, WEB | 플랫폼 타입 |
| `app_version` | string | * | max: 50 | 앱 버전 (예: 1.0.0) |
| `os_version` | string | * | max: 50 | OS 버전 (예: 14) |

**Request Example:**
```json
{
  "device_id": "android-device-123",
  "fcm_token": "fcm-token-xxxxxxxxxxxxxxxxxxxxxxxxx",
  "platform": "ANDROID",
  "app_version": "1.0.0",
  "os_version": "14"
}
```

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | - | 디바이스 레코드 ID |
| `device_id` | string | N | max: 255 | 기기 고유 ID |
| `platform` | string | N | enum: ANDROID, IOS, WEB | 플랫폼 타입 |
| `app_version` | string | N | max: 50 | 앱 버전 |
| `os_version` | string | N | max: 50 | OS 버전 |
| `is_active` | boolean | N | - | 활성화 여부 (초기값: true) |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |
| `updated_at` | string | N | ISO 8601 format | 수정 시각 |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "id": 5,
    "device_id": "android-device-123",
    "platform": "ANDROID",
    "app_version": "1.0.0",
    "os_version": "14",
    "is_active": true,
    "created_at": "2026-04-10T10:00:00Z",
    "updated_at": "2026-04-10T10:00:00Z"
  },
  "error": null
}
```

**Error Cases:**
- `400 INVALID_REQUEST`: 잘못된 요청 (필수 필드 누락 또는 잘못된 platform 값)

### 7.2 디바이스 비활성화

**Endpoint:** `PATCH /api/v1/devices/{deviceId}/deactivate`

**설명:** 디바이스를 비활성화하여 푸시 알림 수신을 중지합니다. 삭제하지 않고 비활성화 상태로 변경합니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `deviceId` | string | * | max: 255 | 디바이스 ID |

**Request Body:** 없음

**Response 200 OK:**
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

**Error Cases:**
- `404 DEVICE_NOT_FOUND`: 디바이스를 찾을 수 없음

---

## 8. Analytics API

### 8.1 주간 통계 조회

**Endpoint:** `GET /api/v1/analytics/weekly`

**설명:** 최근 7일간의 알림 통계를 조회합니다. LLM이 생성한 인사이트를 포함합니다.

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `total_notifications` | int | N | min: 0 | 총 알림 개수 (7일간) |
| `unread_notifications` | int | N | min: 0 | 미읽음 알림 개수 |
| `source_distribution` | object | N | - | 소스별 알림 개수 (key: NotificationSource, value: int) |
| `priority_distribution` | object | N | - | 우선순위별 알림 개수 (key: NotificationPriority, value: int) |
| `daily_trend` | object | N | - | 일별 알림 개수 (key: YYYY-MM-DD, value: int) |
| `insight` | string | N | max: 2000 | LLM이 생성한 주간 인사이트 |

**source_distribution 객체:**

각 키는 NotificationSource enum 값이며, 값은 해당 소스의 알림 개수입니다.

```typescript
{
  "SLACK": number,
  "GITHUB": number,
  "CLAUDE": number,
  "GMAIL": number,
  "INTERNAL": number
}
```

**priority_distribution 객체:**

각 키는 NotificationPriority enum 값이며, 값은 해당 우선순위의 알림 개수입니다.

```typescript
{
  "URGENT": number,
  "HIGH": number,
  "MEDIUM": number,
  "LOW": number
}
```

**daily_trend 객체:**

각 키는 날짜(YYYY-MM-DD 형식)이며, 값은 해당 날짜의 알림 개수입니다. 최근 7일간의 데이터를 포함합니다.

```typescript
{
  "2026-04-04": number,
  "2026-04-05": number,
  // ... (7일간)
}
```

**Response Example:**
```json
{
  "success": true,
  "data": {
    "total_notifications": 68,
    "unread_notifications": 12,
    "source_distribution": {
      "SLACK": 28,
      "GITHUB": 22,
      "CLAUDE": 12,
      "GMAIL": 6
    },
    "priority_distribution": {
      "URGENT": 8,
      "HIGH": 18,
      "MEDIUM": 32,
      "LOW": 10
    },
    "daily_trend": {
      "2026-04-04": 8,
      "2026-04-05": 12,
      "2026-04-06": 10,
      "2026-04-07": 15,
      "2026-04-08": 9,
      "2026-04-09": 7,
      "2026-04-10": 7
    },
    "insight": "이번 주는 지난 주 대비 알림이 23% 증가했어요. Slack에서 가장 많은 알림을 받았으며, 주로 평일 오후 2-4시에 집중되어 있습니다."
  },
  "error": null
}
```

**Error Cases:**
- `503 LLM_UNAVAILABLE`: LLM 서비스 사용 불가 (insight 생성 실패 시)

---

## 9. 에러 코드

### 9.1 에러 코드 목록

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `NOTIFICATION_NOT_FOUND` | 404 | 알림을 찾을 수 없습니다 |
| `TODO_NOT_FOUND` | 404 | 할일을 찾을 수 없습니다 |
| `DEVICE_NOT_FOUND` | 404 | 디바이스를 찾을 수 없습니다 |
| `WEBHOOK_VERIFICATION_FAILED` | 401 | Webhook 서명 검증에 실패했습니다 |
| `UNSUPPORTED_SOURCE` | 400 | 지원하지 않는 알림 소스입니다 |
| `INVALID_REQUEST` | 400 | 잘못된 요청입니다 |
| `LLM_UNAVAILABLE` | 503 | LLM 서비스를 사용할 수 없습니다 |
| `EMBEDDING_FAILED` | 500 | 임베딩 생성에 실패했습니다 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류가 발생했습니다 |

### 9.2 에러 응답 예시

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "NOTIFICATION_NOT_FOUND",
    "message": "알림을 찾을 수 없습니다"
  }
}
```

---

## 부록

### A. 데이터 타입 상세

**Notification 객체:**
```typescript
{
  id: number;                          // 알림 ID
  source: NotificationSource;          // enum: CLAUDE, SLACK, GITHUB, GMAIL, INTERNAL
  title: string;                       // max: 255
  body: string;                        // max: 2000
  priority: NotificationPriority;      // enum: URGENT, HIGH, MEDIUM, LOW
  is_read: boolean;                    // 읽음 여부
  created_at: string;                  // ISO 8601 format
  updated_at: string;                  // ISO 8601 format
  external_id: string | null;          // max: 255
  external_url: string | null;         // max: 500
  metadata: Record<string, any> | null;// JSON 객체
}
```

**ChatMessage 객체:**
```typescript
{
  id: number;                // 메시지 ID
  role: MessageRole;         // enum: USER, ASSISTANT
  content: string;           // max: 4000
  created_at: string;        // ISO 8601 format
}
```

**Todo 객체:**
```typescript
{
  id: number;                // 할일 ID
  notification_id: number | null;  // 연관 알림 ID
  title: string;             // max: 255
  description: string | null;// max: 1000
  status: TodoStatus;        // enum: PENDING, IN_PROGRESS, DONE
  due_date: string | null;   // ISO 8601 format
  created_at: string;        // ISO 8601 format
  updated_at: string;        // ISO 8601 format
}
```

**Device 객체:**
```typescript
{
  id: number;                // 디바이스 레코드 ID
  device_id: string;         // max: 255
  fcm_token: string;         // max: 500 (Response에는 포함되지 않음 - 보안)
  platform: "ANDROID" | "IOS" | "WEB";
  app_version: string;       // max: 50
  os_version: string;        // max: 50
  is_active: boolean;        // 활성화 여부
  created_at: string;        // ISO 8601 format
  updated_at: string;        // ISO 8601 format
}
```

**WeeklyAnalytics 객체:**
```typescript
{
  total_notifications: number;           // min: 0
  unread_notifications: number;          // min: 0
  source_distribution: {                 // 소스별 개수
    [key in NotificationSource]: number;
  };
  priority_distribution: {               // 우선순위별 개수
    [key in NotificationPriority]: number;
  };
  daily_trend: {                         // 일별 개수 (7일간)
    [date: string]: number;              // key: YYYY-MM-DD
  };
  insight: string;                       // max: 2000, LLM 생성
}
```

**DailySummary 객체:**
```typescript
{
  summary: string;           // max: 2000
  date: string;              // YYYY-MM-DD format
  total_messages: number;    // min: 0
  topics: string[];          // max items: 10, 각 항목 max: 100
}
```

**ApiResponse<T> 제네릭 타입:**
```typescript
// 성공 응답
{
  success: true;
  data: T;                   // 실제 데이터
  error: null;
}

// 에러 응답
{
  success: false;
  data: null;
  error: {
    code: string;            // ErrorCode enum
    message: string;         // 에러 메시지
  };
}
```

**Paginated<T> 제네릭 타입:**
```typescript
{
  content: T[];              // 항목 배열
  page: number;              // 현재 페이지 (0-based)
  size: number;              // 페이지 크기
  totalElements: number;     // 전체 항목 수
  totalPages: number;        // 전체 페이지 수
  first: boolean;            // 첫 페이지 여부
  last: boolean;             // 마지막 페이지 여부
}
```

### B. 환경변수

백엔드 서비스는 다음 환경변수를 사용합니다:

```bash
# Database
NOTIO_DB_URL=jdbc:postgresql://localhost:5432/notio
NOTIO_DB_USER=notio
NOTIO_DB_PASSWORD=notio

# Redis
NOTIO_REDIS_HOST=localhost
NOTIO_REDIS_PORT=6379

# Ollama (Phase 1+)
NOTIO_OLLAMA_URL=http://localhost:11434
NOTIO_LLM_MODEL=llama3.2:3b
NOTIO_EMBED_MODEL=nomic-embed-text

# JWT (Phase 2+)
NOTIO_JWT_SECRET=your-secret-key
NOTIO_JWT_EXPIRY_MS=86400000

# Webhook Secrets
NOTIO_SLACK_SECRET=your-slack-signing-secret
NOTIO_GITHUB_SECRET=your-github-webhook-secret
NOTIO_INTERNAL_TOKEN=your-internal-token

# Firebase
FIREBASE_CREDENTIALS_PATH=/path/to/firebase-service-account.json

# RAG
NOTIO_EMBED_DIM=768
NOTIO_RAG_TOP_K=5
```

### C. Validation 규칙 요약

**문자열 길이 제한:**

| 필드 | 최소 | 최대 |
|------|------|------|
| Notification.title | 1 | 255 |
| Notification.body | 1 | 2000 |
| Notification.external_id | - | 255 |
| Notification.external_url | - | 500 |
| ChatMessage.content | 1 | 4000 |
| Chat request content | 1 | 1000 |
| Todo.title | 1 | 255 |
| Todo.description | - | 1000 |
| Device.device_id | 1 | 255 |
| Device.fcm_token | 1 | 500 |
| Device.app_version | 1 | 50 |
| Device.os_version | 1 | 50 |
| DailySummary.summary | 1 | 2000 |
| WeeklyAnalytics.insight | 1 | 2000 |

**숫자 범위 제한:**

| 필드 | 최소 | 최대 |
|------|------|------|
| page | 0 | - |
| size | 1 | 100 |
| notification_id | 1 | - |
| todo_id | 1 | - |

**날짜/시간 형식:**
- ISO 8601 형식: `2026-04-10T10:30:00Z`
- 날짜만: `2026-04-10` (YYYY-MM-DD)

**Enum 값:**
- NotificationSource: `CLAUDE`, `SLACK`, `GITHUB`, `GMAIL`, `INTERNAL`
- NotificationPriority: `URGENT`, `HIGH`, `MEDIUM`, `LOW`
- TodoStatus: `PENDING`, `IN_PROGRESS`, `DONE`
- MessageRole: `USER`, `ASSISTANT`
- Platform: `ANDROID`, `IOS`, `WEB`

### D. 버전 히스토리

| 버전 | 날짜 | 변경사항 |
|------|------|----------|
| 1.0 | 2026-04-10 | 초기 API 명세서 작성 (Phase 0 MVP 기준) |
| 1.1 | 2026-04-10 | Request/Response 필드 상세 명세 추가 (필수/선택, 타입, 제약사항) |
| 1.2 | 2026-04-10 | Push API 7.1 Response에 app_version, os_version 필드 추가, Platform enum에 WEB 추가, 7.2 디바이스 비활성화 API 추가 |

---

**문서 끝**

이 문서는 Notio 프로젝트의 공식 API 명세서입니다.
백엔드 개발 시 이 명세를 기준으로 구현하고, 프론트엔드에서는 이 명세에 맞춰 API를 호출합니다.
API 변경 시 이 문서를 먼저 업데이트한 후 구현해야 합니다.
