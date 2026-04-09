# Notio API 명세서

> 버전: v0.2  
> 기준 문서: `docs/blueprint/notio_blueprint.md`, `docs/tasks/task_backend.md`, `frontend/lib/**`  
> 서버 주소: `http://{SERVER_URL}`  
> 기본 Prefix: `/api/v1`

## 1. API 개요

Notio API는 개발자를 위한 통합 알림 허브 백엔드 API입니다. Claude Code, Slack, GitHub 등의 외부 이벤트를 webhook으로 수신하고, 이를 알림으로 저장한 뒤 조회, 읽음 처리, AI 채팅, 할일 생성, 분석, 디바이스 등록 기능을 제공합니다.

### 1.1 API 용도

- 외부 서비스 webhook 수신
- 통합 알림 목록/상세/읽음/삭제 처리
- AI 채팅 및 일일 요약 제공
- 알림 기반 Todo 생성 및 관리
- 주간 분석 데이터 제공
- 모바일 푸시 디바이스 등록

### 1.2 API 유형

- REST API 기반
- 일부 채팅 응답은 SSE(`text/event-stream`) 기반 스트리밍 사용 예정
- JSON 요청/응답 사용

### 1.3 요청 방식

- `GET`: 조회
- `POST`: 생성, 명시적 액션 수행
- `PATCH`: 부분 수정, 읽음 처리, 상태 변경
- `DELETE`: 삭제

### 1.4 구현 상태 기준

- `구현됨`: 현재 backend 코드에 존재하거나 실행 확인된 API
- `예정`: task 문서 또는 frontend 코드에서 기대하지만 아직 미구현인 API

## 2. 인증(Authentication)과 권한 부여(Authorization)

### 2.1 일반 API

Phase 0 기준으로 인증 체계는 완성 전입니다. 다만 장기적으로는 JWT 기반 인증을 사용할 예정입니다.

예상 Header:

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| Authorization | String | 선택/향후 필수 | `Bearer {ACCESS_TOKEN}` |
| Content-Type | String | 선택 | `application/json` |

### 2.2 Webhook API

Webhook는 소스별 검증 방식을 사용합니다.

| 소스 | 인증 방식 | 헤더 |
| --- | --- | --- |
| Claude | Bearer Token | `Authorization: Bearer {TOKEN}` |
| Slack | HMAC-SHA256 | `X-Slack-Signature`, `X-Slack-Request-Timestamp` |
| GitHub | HMAC-SHA256 | `X-Hub-Signature-256` |

### 2.3 권한 부여

- 일반 사용자 권한 모델은 Phase 4 Auth Service에서 본격화 예정
- 현재 문서 기준으로는 엔드포인트별 세부 Role 정책은 아직 정의되지 않음

## 3. 공통 규칙

### 3.1 공통 응답 형식

성공 응답:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "meta": null
}
```

실패 응답:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "잘못된 요청입니다.",
    "details": {
      "reason": "지원하지 않는 source 입니다."
    }
  },
  "meta": null
}
```

### 3.2 데이터 규칙

- 요청/응답 본문: JSON
- 필드명: `snake_case`
- 시간: ISO-8601 문자열
- 페이지네이션 기본 파라미터: `page`, `size`

### 3.3 공통 에러 코드

| 코드 | HTTP 상태 | 설명 |
| --- | --- | --- |
| INVALID_REQUEST | 400 | 잘못된 요청 |
| UNAUTHORIZED | 401 | 인증 실패 |
| FORBIDDEN | 403 | 접근 권한 없음 |
| RESOURCE_NOT_FOUND | 404 | 리소스 없음 |
| EXTERNAL_PUSH_FAILED | 502 | 외부 푸시 연동 실패 |
| AI_SERVICE_UNAVAILABLE | 503 | AI 서비스 사용 불가 |
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류 |

## 4. 자원(Resource) 모델

## 4.1 Notification 자원

알림은 Notio의 핵심 자원입니다. Webhook 또는 내부 이벤트로 생성되며, 읽음 처리와 소프트 삭제가 가능합니다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| id | Integer | 알림 고유 ID |
| source | String | 알림 소스 |
| title | String | 제목 |
| body | String | 본문 |
| priority | String | 우선순위 |
| is_read | Boolean | 읽음 여부 |
| created_at | String | 생성 시각 |
| external_id | String | 외부 시스템 ID |
| external_url | String | 외부 URL |
| metadata | Object | 추가 메타데이터 |

상태 전이:

- 생성됨
- 읽지 않음
- 읽음 처리
- 소프트 삭제

예시:

```json
{
  "id": 1,
  "source": "SLACK",
  "title": "#dev-team 채널에 멘션",
  "body": "@cookyuu PR 리뷰 부탁드립니다.",
  "priority": "HIGH",
  "is_read": false,
  "created_at": "2026-04-09T22:10:00Z",
  "external_id": "slack-msg-001",
  "external_url": "https://...",
  "metadata": {
    "channel": "dev-team",
    "user": "박철수"
  }
}
```

### 4.2 ChatMessage 자원

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| id | Integer | 메시지 고유 ID |
| role | String | `user`, `assistant` |
| content | String | 메시지 내용 |
| created_at | String | 생성 시각 |

예시:

```json
{
  "id": 101,
  "role": "assistant",
  "content": "오늘은 Slack과 GitHub 알림이 집중되었습니다.",
  "created_at": "2026-04-09T22:10:00Z"
}
```

### 4.3 DailySummary 자원

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| summary | String | 요약 문장 |
| date | String | 요약 대상 날짜 |
| total_messages | Integer | 반영된 메시지 수 |
| topics | Array[String] | 주요 토픽 목록 |

### 4.4 Todo 자원

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| id | Integer | Todo ID |
| title | String | 할일 제목 |
| description | String | 설명 |
| status | String | `PENDING`, `IN_PROGRESS`, `DONE` |
| notification_id | Integer | 연관 알림 ID |
| created_at | String | 생성 시각 |
| updated_at | String | 수정 시각 |

### 4.5 WeeklyAnalytics 자원

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| total_notifications | Integer | 전체 알림 수 |
| unread_notifications | Integer | 미읽음 수 |
| source_distribution | Object | 소스별 분포 |
| priority_distribution | Object | 우선순위별 분포 |
| daily_trend | Object | 일자별 알림 수 |
| insight | String | AI 또는 집계 기반 인사이트 |

### 4.6 Device 자원

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| platform | String | `ANDROID` 등 플랫폼 |
| token | String | FCM 토큰 |
| device_name | String | 사용자 디바이스 이름 |

## 5. 엔드포인트 요약

| 구분 | API 명 | 메서드 | URL | 상태 |
| --- | --- | --- | --- | --- |
| Webhook | Webhook 수신 | POST | `/api/v1/webhook/{source}` | 구현됨 |
| Notification | 알림 목록 조회 | GET | `/api/v1/notifications` | 예정 |
| Notification | 알림 상세 조회 | GET | `/api/v1/notifications/{id}` | 예정 |
| Notification | 알림 읽음 처리 | PATCH | `/api/v1/notifications/{id}/read` | 예정 |
| Notification | 전체 읽음 처리 | POST 또는 PATCH | `/api/v1/notifications/read-all` | 예정 |
| Notification | 미읽음 개수 조회 | GET | `/api/v1/notifications/unread-count` | 예정 |
| Notification | 알림 삭제 | DELETE | `/api/v1/notifications/{id}` | 예정 |
| Chat | 채팅 요청 | POST | `/api/v1/chat` | 예정 |
| Chat | 채팅 스트리밍 | GET | `/api/v1/chat/stream` | 예정 |
| Chat | 일일 요약 조회 | GET | `/api/v1/chat/daily-summary` | 예정 |
| Chat | 채팅 이력 조회 | GET | `/api/v1/chat/history` | 예정 |
| Todo | Todo 생성 | POST | `/api/v1/todos` | 예정 |
| Todo | Todo 목록 조회 | GET | `/api/v1/todos` | 예정 |
| Todo | Todo 수정 | PATCH | `/api/v1/todos/{id}` | 예정 |
| Analytics | 주간 분석 조회 | GET | `/api/v1/analytics/weekly` | 예정 |
| Device | 디바이스 등록 | POST | `/api/v1/devices/register` | 예정 |
| Auth | 로그인 | POST | `/api/v1/auth/login` | 예정 |
| Auth | 토큰 재발급 | POST | `/api/v1/auth/refresh` | 예정 |
| Auth | 로그아웃 | POST | `/api/v1/auth/logout` | 예정 |

## 6. 엔드포인트 상세

## 6.1 Webhook 수신

외부 서비스에서 전달된 webhook을 검증하고 내부 `NotificationEvent`로 변환합니다.

### Request

#### Request Syntax

Claude:

```bash
curl -X POST http://{SERVER_URL}/api/v1/webhook/claude \
  -H "Authorization: Bearer {CLAUDE_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
        "event_id": "evt_001",
        "title": "Claude Code 작업 완료",
        "body": "리팩터링이 완료되었습니다."
      }'
```

Slack:

```bash
curl -X POST http://{SERVER_URL}/api/v1/webhook/slack \
  -H "X-Slack-Signature: {SIGNATURE}" \
  -H "X-Slack-Request-Timestamp: {TIMESTAMP}" \
  -H "Content-Type: application/json" \
  -d '{ "type": "event_callback" }'
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| POST | `http://{SERVER_URL}/api/v1/webhook/{source}` |

#### Request Header

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| Authorization | String | Claude 시 필수 | Bearer token |
| X-Slack-Signature | String | Slack 시 필수 | Slack 서명 |
| X-Slack-Request-Timestamp | String | Slack 시 필수 | Slack 요청 시각 |
| X-Hub-Signature-256 | String | GitHub 시 필수 | GitHub 서명 |
| Content-Type | String | 필수 | `application/json` |

#### Path Parameter

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| source | String | 필수 | `claude`, `slack`, `github` |

#### Request Body

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| body | Object | 필수 | 외부 provider 원본 JSON payload |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "notification_id": 1,
    "received": true
  },
  "error": null,
  "meta": null
}
```

#### Response Elements

| 필드 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| success | Boolean | 필수 | 성공 여부 |
| data.notification_id | Integer | 필수 | 생성 또는 수신된 알림 ID |
| data.received | Boolean | 필수 | webhook 수신 여부 |

### 에러 처리

| HTTP 상태 | 코드 | 설명 |
| --- | --- | --- |
| 400 | INVALID_REQUEST | source 오류 또는 잘못된 JSON |
| 401 | UNAUTHORIZED | webhook 검증 실패 |
| 500 | INTERNAL_SERVER_ERROR | 내부 처리 실패 |

## 6.2 알림 목록 조회

알림 목록을 페이지 단위로 조회합니다.

### Request

#### Request Syntax

```bash
curl -X GET "http://{SERVER_URL}/api/v1/notifications?source=SLACK&page=0&size=20" \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| GET | `http://{SERVER_URL}/api/v1/notifications` |

#### Request Header

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| Authorization | String | 향후 필수 | 액세스 토큰 |

#### Request Parameter

| 파라미터 | 타입 | 필수여부 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| source | String | 선택 | 없음 | 알림 소스 필터 |
| page | Integer | 선택 | 0 | 페이지 번호 |
| size | Integer | 선택 | 20 | 페이지 크기 |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "source": "SLACK",
      "title": "#dev-team 채널에 멘션",
      "body": "@cookyuu PR 리뷰 부탁드립니다.",
      "priority": "HIGH",
      "is_read": false,
      "created_at": "2026-04-09T22:10:00Z",
      "external_id": "slack-msg-001",
      "external_url": "https://...",
      "metadata": {
        "channel": "dev-team"
      }
    }
  ],
  "error": null,
  "meta": {
    "page": 0,
    "size": 20,
    "has_next": false,
    "total_elements": 1
  }
}
```

#### Response Elements

| 필드 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| success | Boolean | 필수 | 성공 여부 |
| data | Array[Notification] | 필수 | 알림 목록 |
| meta.page | Integer | 필수 | 현재 페이지 |
| meta.size | Integer | 필수 | 페이지 크기 |
| meta.has_next | Boolean | 필수 | 다음 페이지 존재 여부 |
| meta.total_elements | Integer | 필수 | 전체 알림 수 |

### 에러 처리

| HTTP 상태 | 코드 | 설명 |
| --- | --- | --- |
| 400 | INVALID_REQUEST | 잘못된 필터 또는 페이지 파라미터 |
| 401 | UNAUTHORIZED | 인증 실패 |

## 6.3 알림 상세 조회

개별 알림을 조회합니다. task 문서상 MVP에서는 상세 조회 시 자동 읽음 처리 가능성이 있습니다.

### Request

#### Request Syntax

```bash
curl -X GET http://{SERVER_URL}/api/v1/notifications/1 \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| GET | `http://{SERVER_URL}/api/v1/notifications/{id}` |

#### Path Parameter

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| id | Integer | 필수 | 알림 ID |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "id": 1,
    "source": "SLACK",
    "title": "#dev-team 채널에 멘션",
    "body": "@cookyuu PR 리뷰 부탁드립니다.",
    "priority": "HIGH",
    "is_read": true,
    "created_at": "2026-04-09T22:10:00Z",
    "external_id": "slack-msg-001",
    "external_url": "https://...",
    "metadata": {
      "channel": "dev-team"
    }
  },
  "error": null,
  "meta": null
}
```

### 에러 처리

| HTTP 상태 | 코드 | 설명 |
| --- | --- | --- |
| 404 | RESOURCE_NOT_FOUND | 존재하지 않는 알림 |

## 6.4 알림 읽음 처리

개별 알림을 읽음 상태로 변경합니다.

### Request

#### Request Syntax

```bash
curl -X PATCH http://{SERVER_URL}/api/v1/notifications/1/read \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| PATCH | `http://{SERVER_URL}/api/v1/notifications/{id}/read` |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "id": 1,
    "is_read": true
  },
  "error": null,
  "meta": null
}
```

## 6.5 전체 알림 읽음 처리

모든 미읽음 알림을 읽음 상태로 변경합니다.

### Request

#### Request Syntax

```bash
curl -X POST http://{SERVER_URL}/api/v1/notifications/read-all \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| POST | `http://{SERVER_URL}/api/v1/notifications/read-all` |

주의:

- backend task 문서는 `POST`
- frontend mock datasource는 `PATCH`
- 실제 구현 전 둘 중 하나로 통일 필요

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "updated_count": 12
  },
  "error": null,
  "meta": null
}
```

## 6.6 미읽음 개수 조회

미읽음 알림 개수를 조회합니다.

### Request

#### Request Syntax

```bash
curl -X GET http://{SERVER_URL}/api/v1/notifications/unread-count \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| GET | `http://{SERVER_URL}/api/v1/notifications/unread-count` |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "count": 12
  },
  "error": null,
  "meta": null
}
```

## 6.7 알림 삭제

알림을 소프트 삭제합니다.

### Request

#### Request Syntax

```bash
curl -X DELETE http://{SERVER_URL}/api/v1/notifications/1 \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| DELETE | `http://{SERVER_URL}/api/v1/notifications/{id}` |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "deleted": true
  },
  "error": null,
  "meta": null
}
```

## 6.8 채팅 요청

사용자 질문을 전송하고 AI 응답 메시지를 반환합니다.

### Request

#### Request Syntax

```bash
curl -X POST http://{SERVER_URL}/api/v1/chat \
  -H "Authorization: Bearer {ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
        "content": "오늘 중요한 알림 요약해줘"
      }'
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| POST | `http://{SERVER_URL}/api/v1/chat` |

#### Request Elements

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| content | String | 필수 | 사용자 질문 |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "message": {
      "id": 101,
      "role": "assistant",
      "content": "오늘은 GitHub PR 리뷰 요청이 중요합니다.",
      "created_at": "2026-04-09T22:10:00Z"
    }
  },
  "error": null,
  "meta": null
}
```

## 6.9 채팅 스트리밍

SSE로 AI 응답을 청크 단위로 수신합니다.

### Request

#### Request Syntax

```bash
curl -N -X GET "http://{SERVER_URL}/api/v1/chat/stream?content=오늘%20요약해줘" \
  -H "Authorization: Bearer {ACCESS_TOKEN}" \
  -H "Accept: text/event-stream"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| GET | `http://{SERVER_URL}/api/v1/chat/stream` |

#### Request Parameter

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| content | String | 필수 | 사용자 질문 |

### Response

#### Response Syntax

```text
event: chunk
data: 오늘은

event: chunk
data: GitHub PR 리뷰 요청이 중요합니다.

event: done
data: [DONE]
```

## 6.10 일일 요약 조회

오늘의 알림/채팅 요약을 조회합니다.

### Request

#### Request Syntax

```bash
curl -X GET http://{SERVER_URL}/api/v1/chat/daily-summary \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| GET | `http://{SERVER_URL}/api/v1/chat/daily-summary` |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "summary": "오늘은 PR 리뷰 요청과 Slack 멘션이 많았습니다.",
    "date": "2026-04-09",
    "total_messages": 18,
    "topics": ["PR review", "Slack mention", "build failure"]
  },
  "error": null,
  "meta": null
}
```

## 6.11 채팅 이력 조회

채팅 메시지 이력을 조회합니다.

### Request

#### Request Syntax

```bash
curl -X GET "http://{SERVER_URL}/api/v1/chat/history?page=0&size=20" \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| GET | `http://{SERVER_URL}/api/v1/chat/history` |

#### Request Parameter

| 파라미터 | 타입 | 필수여부 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| page | Integer | 선택 | 0 | 페이지 번호 |
| size | Integer | 선택 | 20 | 페이지 크기 |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "role": "user",
      "content": "오늘 요약해줘",
      "created_at": "2026-04-09T21:00:00Z"
    },
    {
      "id": 2,
      "role": "assistant",
      "content": "오늘은 Slack 알림이 가장 많았습니다.",
      "created_at": "2026-04-09T21:00:02Z"
    }
  ],
  "error": null,
  "meta": {
    "page": 0,
    "size": 20,
    "has_next": false
  }
}
```

## 6.12 Todo 생성

알림 기반으로 Todo를 생성합니다.

### Request

#### Request Syntax

```bash
curl -X POST http://{SERVER_URL}/api/v1/todos \
  -H "Authorization: Bearer {ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
        "notification_id": 1,
        "title": "PR #456 리뷰하기"
      }'
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| POST | `http://{SERVER_URL}/api/v1/todos` |

#### Request Elements

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| notification_id | Integer | 필수 | 연결된 알림 ID |
| title | String | 선택 | 미입력 시 LLM 생성 가능 |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "id": 201,
    "title": "PR #456 리뷰하기",
    "description": "Slack 멘션 기반 자동 생성",
    "status": "PENDING",
    "notification_id": 1,
    "created_at": "2026-04-09T22:10:00Z",
    "updated_at": "2026-04-09T22:10:00Z"
  },
  "error": null,
  "meta": null
}
```

## 6.13 Todo 목록 조회

Todo 목록을 조회합니다.

### Request

#### Request Syntax

```bash
curl -X GET "http://{SERVER_URL}/api/v1/todos?status=PENDING&page=0&size=20" \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### Request Parameter

| 파라미터 | 타입 | 필수여부 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| status | String | 선택 | 없음 | Todo 상태 |
| page | Integer | 선택 | 0 | 페이지 번호 |
| size | Integer | 선택 | 20 | 페이지 크기 |

## 6.14 Todo 수정

Todo 상태 또는 내용을 변경합니다.

### Request

#### Request Syntax

```bash
curl -X PATCH http://{SERVER_URL}/api/v1/todos/201 \
  -H "Authorization: Bearer {ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
        "status": "DONE"
      }'
```

#### Request Elements

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| status | String | 선택 | 변경할 Todo 상태 |
| title | String | 선택 | 수정할 제목 |
| description | String | 선택 | 수정할 설명 |

## 6.15 주간 분석 조회

이번 주 알림 통계를 조회합니다.

### Request

#### Request Syntax

```bash
curl -X GET http://{SERVER_URL}/api/v1/analytics/weekly \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

#### 메서드 / 요청 URL

| 메서드 | 요청 URL |
| --- | --- |
| GET | `http://{SERVER_URL}/api/v1/analytics/weekly` |

### Response

#### Response Syntax

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
      "HIGH": 18,
      "MEDIUM": 32,
      "LOW": 18
    },
    "daily_trend": {
      "2026-04-03": 5,
      "2026-04-04": 12
    },
    "insight": "이번 주는 지난 주 대비 알림이 증가했습니다."
  },
  "error": null,
  "meta": null
}
```

## 6.16 디바이스 등록

모바일 푸시 수신을 위한 FCM 디바이스 토큰을 등록합니다.

### Request

#### Request Syntax

```bash
curl -X POST http://{SERVER_URL}/api/v1/devices/register \
  -H "Authorization: Bearer {ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
        "platform": "ANDROID",
        "token": "fcm_device_token",
        "device_name": "Pixel 8"
      }'
```

#### Request Elements

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| platform | String | 필수 | 디바이스 플랫폼 |
| token | String | 필수 | FCM 토큰 |
| device_name | String | 선택 | 디바이스 이름 |

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "registered": true
  },
  "error": null,
  "meta": null
}
```

## 6.17 로그인

사용자 로그인 API입니다. frontend는 현재 raw DTO 형식을 기대하고 있으나, 백엔드 표준은 `ApiResponse<T>` 입니다.

### Request

#### Request Syntax

```bash
curl -X POST http://{SERVER_URL}/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
        "email": "user@example.com",
        "password": "password"
      }'
```

#### Request Elements

| 파라미터 | 타입 | 필수여부 | 설명 |
| --- | --- | --- | --- |
| email | String | 필수 | 이메일 |
| password | String | 필수 | 비밀번호 |

### Response

#### Response Syntax

권장 표준 응답:

```json
{
  "success": true,
  "data": {
    "user_id": "u_123",
    "email": "user@example.com",
    "access_token": "jwt_access_token",
    "refresh_token": "jwt_refresh_token",
    "expires_in": 3600
  },
  "error": null,
  "meta": null
}
```

frontend 현재 기대 형식:

```json
{
  "user_id": "u_123",
  "email": "user@example.com",
  "access_token": "jwt_access_token",
  "refresh_token": "jwt_refresh_token",
  "expires_in": 3600
}
```

## 6.18 토큰 재발급

### Request

#### Request Syntax

```bash
curl -X POST http://{SERVER_URL}/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
        "refresh_token": "jwt_refresh_token"
      }'
```

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "access_token": "new_access_token",
    "refresh_token": "new_refresh_token",
    "expires_in": 3600
  },
  "error": null,
  "meta": null
}
```

## 6.19 로그아웃

### Request

#### Request Syntax

```bash
curl -X POST http://{SERVER_URL}/api/v1/auth/logout \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

### Response

#### Response Syntax

```json
{
  "success": true,
  "data": {
    "logged_out": true
  },
  "error": null,
  "meta": null
}
```

## 7. 에러(Error) 처리 설명

API 호출 중 예외가 발생하면 공통 에러 형식으로 응답합니다.

예시:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "리소스를 찾을 수 없습니다.",
    "details": {
      "id": 1
    }
  },
  "meta": null
}
```

대표 처리 방식:

- 입력값 오류: `400 INVALID_REQUEST`
- 인증 실패: `401 UNAUTHORIZED`
- 리소스 없음: `404 RESOURCE_NOT_FOUND`
- 외부 시스템 실패: `502 EXTERNAL_PUSH_FAILED`
- AI 서비스 불가: `503 AI_SERVICE_UNAVAILABLE`
- 미처리 예외: `500 INTERNAL_SERVER_ERROR`

## 8. API 예제 사용 시 주의사항

- Webhook API는 일반 Bearer 인증이 아니라 소스별 검증 방식을 사용합니다.
- Auth API는 현재 frontend가 raw DTO를 기대하므로 응답 포맷 정렬이 필요합니다.
- `notifications/read-all` 메서드는 `POST`와 `PATCH` 사이에서 확정이 필요합니다.
- `GET /notifications/{id}`의 자동 읽음 처리 여부는 구현 전에 정책 확정이 필요합니다.

