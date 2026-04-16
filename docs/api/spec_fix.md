# Notio Fix API 명세서

> **버전**: v1.4-fix
> **Base URL**: `http://localhost:8080` (로컬 개발)
> **API 버전**: `/api/v1`
> **최종 수정**: 2026-04-16

---

## 중요 사항

### 이 명세서의 목적

이 문서는 `docs/plans/plan_fix.md`의 Connections 기반 Webhook/Notification 보안 수정에 대한 **Backend와 Frontend 개발 계약(Contract)** 입니다.

기존 전체 API 명세는 `docs/api/spec.md`를 따릅니다. 이 문서는 fix 범위에서 추가되거나 변경되는 API 계약을 우선 정의합니다.

**Backend 개발자:**
- 이 명세에 정의된 endpoint, field name, type, nullable, validation, status code, error code를 준수해야 합니다.
- Connection과 Notification은 반드시 로그인 사용자 기준으로 격리해야 합니다.
- Webhook 인증은 사용자 JWT가 아니라 provider별 인증 정책과 connection credential 검증으로 처리해야 합니다.
- API Key 원문은 생성 또는 rotate 응답에서만 1회 반환해야 합니다.

**Frontend 개발자:**
- 이 명세를 기준으로 Request DTO와 Response DTO를 정의할 수 있습니다.
- 원문 API Key는 화면 표시와 복사 용도로만 사용하고 저장하지 않아야 합니다.
- `429`, `413`, connection 관련 error code를 사용자에게 일관된 방식으로 표시해야 합니다.

**중요:** API 변경이 필요한 경우:
1. 이 명세서를 먼저 수정
2. Backend/Frontend 팀이 변경사항 리뷰
3. 합의 후 구현 시작

---

## 목차

1. [공통 사항](#1-공통-사항)
2. [인증 경계](#2-인증-경계)
3. [공통 타입 정의](#3-공통-타입-정의)
4. [Connection API](#4-connection-api)
5. [OAuth API](#5-oauth-api)
6. [Webhook API](#6-webhook-api)
7. [Notification API 변경사항](#7-notification-api-변경사항)
8. [Rate Limit](#8-rate-limit)
9. [에러 코드](#9-에러-코드)
10. [부록](#10-부록)

---

## 1. 공통 사항

### 1.1 공통 응답 형식

모든 일반 앱 API는 다음 형식의 응답을 반환합니다.

**성공 응답:**
```json
{
  "success": true,
  "data": {},
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

Webhook API도 기본적으로 동일한 응답 형식을 사용합니다. 다만 외부 provider에 상세 실패 사유를 노출하지 않기 위해 webhook 검증 실패는 통합 error code를 사용할 수 있습니다.

### 1.2 HTTP 상태 코드

| 상태 코드 | 설명 |
|----------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 204 | 성공 (응답 본문 없음) |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 404 | 리소스 없음 |
| 409 | 중복 또는 충돌 |
| 413 | 요청 본문 크기 초과 |
| 429 | 요청 횟수 제한 초과 |
| 500 | 서버 내부 오류 |
| 503 | 서비스 사용 불가 |

### 1.3 페이지네이션

목록 조회 API는 다음 쿼리 파라미터를 지원합니다.

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | int | 0 | 페이지 번호 (0부터 시작) |
| `size` | int | 20 | 페이지 크기 |

**페이지네이션 응답:**
```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  },
  "error": null
}
```

### 1.4 필드 명세 규칙

| 표기 | 의미 |
|------|------|
| `*` | 필수 (Required) |
| - | 선택 (Optional) |
| `nullable` | null 값 허용 |
| `min/max` | 최소/최대 길이 또는 값 |
| `default` | 기본값 |

### 1.5 Naming

- JSON field는 기존 API와 동일하게 `snake_case`를 사용합니다.
- enum response value는 backend enum 기준의 `UPPER_SNAKE_CASE`를 사용합니다.
- path variable의 provider 값은 lowercase를 사용합니다.

---

## 2. 인증 경계

### 2.1 일반 앱 API

일반 앱 API는 기존 JWT 인증을 사용합니다.

**Headers:**

| 헤더 | 필수 | 설명 |
|------|------|------|
| `Authorization` | * | `Bearer {ACCESS_JWT}` |

대상:

- `/api/v1/notifications/**`
- `/api/v1/chat/**`
- `/api/v1/todos/**`
- `/api/v1/devices/**`
- `/api/v1/analytics/**`
- `/api/v1/connections/**`

### 2.2 Webhook API

Webhook API는 사용자 JWT와 분리된 인증 경계를 가집니다.

**Headers:**

| 헤더 | 필수 | 설명 |
|------|------|------|
| `Authorization` | provider별 | Claude는 `Bearer {WEBHOOK_API_KEY}` |

대상:

- `/api/v1/webhook/{provider}`

Spring Security에서 webhook path는 `permitAll`일 수 있습니다. 이는 인증 생략이 아니라 사용자 JWT filter 대상에서 제외한다는 의미입니다. 실제 인증은 provider adapter가 수행합니다.

---

## 3. 공통 타입 정의

### 3.1 ConnectionProvider

```text
CLAUDE, SLACK, GMAIL, GITHUB, DISCORD, JIRA, LINEAR, TEAMS
```

### 3.2 ConnectionAuthType

```text
API_KEY, OAUTH, SIGNATURE, SYSTEM
```

### 3.3 ConnectionStatus

```text
PENDING, ACTIVE, NEEDS_ACTION, REVOKED, ERROR
```

### 3.4 ConnectionCapability

```text
WEBHOOK_RECEIVE, TEST_MESSAGE, REFRESH_TOKEN, ROTATE_KEY
```

### 3.5 NotificationSource

기존 enum에 대한 확장 가능성을 유지합니다.

```text
CLAUDE, SLACK, GITHUB, GMAIL, INTERNAL
```

### 3.6 NotificationPriority

```text
URGENT, HIGH, MEDIUM, LOW
```

---

## 4. Connection API

Connection API는 모두 JWT 인증이 필요하며, 로그인 사용자의 connection만 조회하거나 변경할 수 있습니다. 다른 사용자의 connection id에 접근하면 `404 CONNECTION_NOT_FOUND`를 반환합니다.

### 4.1 Connection 목록 조회

**Endpoint:** `GET /api/v1/connections`

**설명:** 로그인 사용자의 외부 연동 목록을 조회합니다.

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 기본값 | 설명 |
|---------|------|------|----------|--------|------|
| `provider` | string | - | enum: ConnectionProvider | - | provider 필터 |
| `status` | string | - | enum: ConnectionStatus | - | 상태 필터 |
| `auth_type` | string | - | enum: ConnectionAuthType | - | 인증 방식 필터 |
| `page` | int | - | min: 0 | 0 | 페이지 번호 |
| `size` | int | - | min: 1, max: 100 | 20 | 페이지 크기 |

**Response 200 OK - 필드 명세:**

`data.content` 배열의 각 connection 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | min: 1 | Connection ID |
| `provider` | string | N | enum: ConnectionProvider | provider |
| `auth_type` | string | N | enum: ConnectionAuthType | 인증 방식 |
| `display_name` | string | N | max: 100 | 사용자가 지정한 표시 이름 |
| `account_label` | string | Y | max: 255 | 외부 계정 표시명 |
| `status` | string | N | enum: ConnectionStatus | connection 상태 |
| `capabilities` | array | N | enum array | 지원 기능 |
| `key_preview` | string | Y | max: 80 | API Key 미리보기 |
| `last_used_at` | string | Y | ISO 8601 format | 마지막 사용 시각 |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |
| `updated_at` | string | N | ISO 8601 format | 수정 시각 |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 12,
        "provider": "SLACK",
        "auth_type": "OAUTH",
        "display_name": "Work Slack",
        "account_label": "notio-dev",
        "status": "ACTIVE",
        "capabilities": ["WEBHOOK_RECEIVE", "TEST_MESSAGE"],
        "key_preview": null,
        "last_used_at": "2026-04-15T00:00:00Z",
        "created_at": "2026-04-10T00:00:00Z",
        "updated_at": "2026-04-10T00:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "error": null
}
```

### 4.2 Connection 상세 조회

**Endpoint:** `GET /api/v1/connections/{id}`

**설명:** 특정 connection 상세 정보를 조회합니다. 원문 API Key는 절대 포함하지 않습니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | Connection ID |

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | min: 1 | Connection ID |
| `provider` | string | N | enum: ConnectionProvider | provider |
| `auth_type` | string | N | enum: ConnectionAuthType | 인증 방식 |
| `display_name` | string | N | max: 100 | 표시 이름 |
| `account_label` | string | Y | max: 255 | 외부 계정 표시명 |
| `external_account_id` | string | Y | max: 255 | provider account id |
| `external_workspace_id` | string | Y | max: 255 | workspace id |
| `subscription_id` | string | Y | max: 255 | subscription id |
| `status` | string | N | enum: ConnectionStatus | 상태 |
| `capabilities` | array | N | enum array | 지원 기능 |
| `metadata` | object | N | JSON object | provider별 metadata |
| `key_preview` | string | Y | max: 80 | API Key 미리보기 |
| `last_used_at` | string | Y | ISO 8601 format | 마지막 사용 시각 |
| `created_at` | string | N | ISO 8601 format | 생성 시각 |
| `updated_at` | string | N | ISO 8601 format | 수정 시각 |

**Error Cases:**
- `404 CONNECTION_NOT_FOUND`: connection을 찾을 수 없음

### 4.3 Connection 생성

**Endpoint:** `POST /api/v1/connections`

**설명:** 새 connection을 생성합니다. `API_KEY` 방식은 생성 응답에서 원문 API Key를 1회만 반환합니다.

**Request Body - 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `provider` | string | * | enum: ConnectionProvider | provider |
| `auth_type` | string | * | enum: ConnectionAuthType | 인증 방식 |
| `display_name` | string | * | min: 1, max: 100 | 표시 이름 |

**Request Example (API Key):**
```json
{
  "provider": "CLAUDE",
  "auth_type": "API_KEY",
  "display_name": "Claude Code - Desktop"
}
```

**Request Example (OAuth):**
```json
{
  "provider": "SLACK",
  "auth_type": "OAUTH",
  "display_name": "Work Slack"
}
```

**Response 201 Created - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `connection` | object | N | 생성된 connection 객체 |
| `api_key` | string | Y | API Key 방식 생성 시 1회 반환 |

`connection` 객체:

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `id` | int | N | min: 1 | Connection ID |
| `provider` | string | N | enum: ConnectionProvider | provider |
| `auth_type` | string | N | enum: ConnectionAuthType | 인증 방식 |
| `display_name` | string | N | max: 100 | 표시 이름 |
| `status` | string | N | enum: ConnectionStatus | 상태 |
| `key_preview` | string | Y | max: 80 | API Key 미리보기 |

**Response Example (API Key):**
```json
{
  "success": true,
  "data": {
    "connection": {
      "id": 13,
      "provider": "CLAUDE",
      "auth_type": "API_KEY",
      "display_name": "Claude Code - Desktop",
      "status": "ACTIVE",
      "key_preview": "ntio_wh_ab12cd34_********"
    },
    "api_key": "ntio_wh_ab12cd34_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
  },
  "error": null
}
```

**Response Example (OAuth):**
```json
{
  "success": true,
  "data": {
    "connection": {
      "id": 14,
      "provider": "SLACK",
      "auth_type": "OAUTH",
      "display_name": "Work Slack",
      "status": "PENDING",
      "key_preview": null
    },
    "api_key": null
  },
  "error": null
}
```

**Error Cases:**
- `400 CONNECTION_PROVIDER_UNSUPPORTED`: 지원하지 않는 provider
- `400 CONNECTION_AUTH_TYPE_UNSUPPORTED`: provider가 지원하지 않는 auth type
- `409 CONNECTION_ALREADY_EXISTS`: 동일 범위 connection이 이미 존재함

### 4.4 Connection 삭제

**Endpoint:** `DELETE /api/v1/connections/{id}`

**설명:** connection을 삭제합니다. 실제 처리는 soft delete 또는 credential revoke로 수행합니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | Connection ID |

**Response 200 OK Example:**
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

**Error Cases:**
- `404 CONNECTION_NOT_FOUND`: connection을 찾을 수 없음

### 4.5 Connection 테스트

**Endpoint:** `POST /api/v1/connections/{id}/test`

**설명:** connection 상태 또는 test message 전송 가능 여부를 확인합니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | Connection ID |

**Request Body:** 없음

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `success` | boolean | N | 테스트 성공 여부 |
| `message` | string | N | 사용자 표시용 메시지 |
| `tested_at` | string | N | ISO 8601 format |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "연동이 정상적으로 동작합니다.",
    "tested_at": "2026-04-16T10:30:00Z"
  },
  "error": null
}
```

**Error Cases:**
- `404 CONNECTION_NOT_FOUND`: connection을 찾을 수 없음
- `401 CONNECTION_VERIFICATION_FAILED`: provider 검증 실패

### 4.6 Connection 갱신

**Endpoint:** `POST /api/v1/connections/{id}/refresh`

**설명:** OAuth token refresh 또는 provider subscription 갱신을 수행합니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | Connection ID |

**Request Body:** 없음

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `id` | int | N | Connection ID |
| `status` | string | N | 갱신 후 상태 |
| `refreshed_at` | string | N | ISO 8601 format |

**Error Cases:**
- `404 CONNECTION_NOT_FOUND`: connection을 찾을 수 없음
- `401 CONNECTION_VERIFICATION_FAILED`: provider 갱신 실패

### 4.7 API Key 재발급

**Endpoint:** `POST /api/v1/connections/{id}/rotate-key`

**설명:** 기존 active API Key를 revoke하고 새 API Key를 발급합니다. 새 원문 key는 응답에서 1회만 반환합니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `id` | int | * | min: 1 | Connection ID |

**Request Body:** 없음

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `connection` | object | N | 갱신된 connection 객체 |
| `api_key` | string | N | 새 원문 API Key |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "connection": {
      "id": 13,
      "provider": "CLAUDE",
      "auth_type": "API_KEY",
      "display_name": "Claude Code - Desktop",
      "status": "ACTIVE",
      "key_preview": "ntio_wh_cd34ef56_********"
    },
    "api_key": "ntio_wh_cd34ef56_yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy"
  },
  "error": null
}
```

**Error Cases:**
- `404 CONNECTION_NOT_FOUND`: connection을 찾을 수 없음
- `400 CONNECTION_AUTH_TYPE_UNSUPPORTED`: API Key rotate를 지원하지 않는 connection

---

## 5. OAuth API

OAuth API는 모두 JWT 인증이 필요합니다.

### 5.1 OAuth URL 생성

**Endpoint:** `POST /api/v1/connections/oauth-url`

**설명:** provider OAuth authorization URL을 생성합니다. backend는 state를 Redis에 저장합니다.

**Request Body - 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `provider` | string | * | enum: SLACK, GMAIL | OAuth provider |
| `display_name` | string | * | min: 1, max: 100 | 생성할 connection 표시 이름 |
| `redirect_uri` | string | - | max: 500 | frontend callback 또는 deep link URI |

**Request Example:**
```json
{
  "provider": "SLACK",
  "display_name": "Work Slack",
  "redirect_uri": "notio://connections/oauth/callback"
}
```

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `authorization_url` | string | N | provider authorization URL |
| `state_expires_in` | int | N | state 만료 시간(초) |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "authorization_url": "https://slack.com/oauth/v2/authorize?client_id=...&state=...",
    "state_expires_in": 600
  },
  "error": null
}
```

**Error Cases:**
- `400 CONNECTION_PROVIDER_UNSUPPORTED`: 지원하지 않는 provider
- `400 CONNECTION_AUTH_TYPE_UNSUPPORTED`: OAuth를 지원하지 않는 provider

### 5.2 OAuth Callback

**Endpoint:** `GET /api/v1/connections/oauth/callback/{provider}`

**설명:** provider OAuth callback을 처리합니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `provider` | string | * | enum: slack, gmail | provider |

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `code` | string | * | min: 1 | authorization code |
| `state` | string | * | min: 1 | OAuth state |

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `connection` | object | N | 생성 또는 갱신된 connection |
| `message` | string | N | 사용자 표시용 메시지 |

**Error Cases:**
- `401 OAUTH_STATE_INVALID`: state가 없거나 만료 또는 불일치
- `400 OAUTH_CALLBACK_FAILED`: provider code 교환 또는 계정 조회 실패
- `400 CONNECTION_PROVIDER_UNSUPPORTED`: 지원하지 않는 provider

---

## 6. Webhook API

Webhook API는 외부 provider가 호출합니다.

### 6.1 Webhook 수신

**Endpoint:** `POST /api/v1/webhook/{provider}`

**설명:** provider별 인증/서명 검증 후 알림을 생성합니다.

**Path Parameters:**

| 파라미터 | 타입 | 필수 | 제약사항 | 설명 |
|---------|------|------|----------|------|
| `provider` | string | * | enum: claude, slack, gmail | provider |

**공통 처리 순서:**

1. Rate limit 통과
2. payload size 제한 통과
3. provider path variable 파싱
4. provider adapter 선택
5. adapter가 요청 인증/서명 검증
6. connection 식별
7. payload를 notification event로 변환
8. connection의 `user_id`로 notification 저장
9. `connection.last_used_at` 갱신
10. connection event 기록

**Response 200 OK - 필드 명세:**

`data` 객체:

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `notification_id` | int | N | 생성된 알림 ID |
| `connection_id` | int | N | 연결된 connection ID |
| `processed_at` | string | N | ISO 8601 format |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "notification_id": 42,
    "connection_id": 13,
    "processed_at": "2026-04-16T10:30:05Z"
  },
  "error": null
}
```

### 6.2 Claude Code Webhook

**인증 방식:** Notio Webhook API Key

**Required Headers:**

| 헤더 | 타입 | 필수 | 검증 방식 |
|------|------|------|-----------|
| `Authorization` | string | * | `Bearer ntio_wh_<prefix>_<secret>` |

**Request Body 필드 명세:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `event_type` | string | * | max: 100 | 이벤트 타입 |
| `notification` | object | * | - | 알림 데이터 |
| `notification.id` | string | * | max: 255 | 외부 알림 ID |
| `notification.title` | string | * | max: 255 | 알림 제목 |
| `notification.message` | string | * | max: 2000 | 알림 메시지 |
| `notification.url` | string | - | max: 500 | 관련 URL |
| `notification.priority` | string | - | enum: urgent, high, medium, low | 우선순위 |
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
    "timestamp": "2026-04-16T10:30:00Z"
  }
}
```

**Error Cases:**
- `401 WEBHOOK_KEY_MISSING`: Authorization header 없음
- `401 WEBHOOK_KEY_INVALID`: key 형식 또는 hash 검증 실패
- `401 WEBHOOK_KEY_EXPIRED`: key 만료
- `401 WEBHOOK_KEY_REVOKED`: key 폐기됨
- `401 WEBHOOK_SOURCE_MISMATCH`: key provider와 path provider 불일치
- `413 PAYLOAD_TOO_LARGE`: body 크기 초과
- `429 RATE_LIMIT_EXCEEDED`: 요청 횟수 제한 초과

### 6.3 Slack Webhook

**인증 방식:** Slack signing secret + `team_id` 기반 connection 매칭

**Required Headers:**

| 헤더 | 타입 | 필수 | 검증 방식 |
|------|------|------|-----------|
| `X-Slack-Signature` | string | * | HMAC-SHA256 서명 |
| `X-Slack-Request-Timestamp` | string | * | replay attack 방지 |

**Request Body 주요 필드:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `type` | string | * | enum: event_callback, url_verification | Slack 이벤트 타입 |
| `team_id` | string | * | max: 50 | Slack workspace/team id |
| `challenge` | string | 조건부 | max: 500 | url_verification 응답값 |
| `event` | object | 조건부 | - | event_callback 데이터 |
| `event.type` | string | 조건부 | max: 100 | 이벤트 서브타입 |
| `event.user` | string | 조건부 | max: 50 | Slack user id |
| `event.text` | string | 조건부 | max: 2000 | 메시지 텍스트 |
| `event.channel` | string | 조건부 | max: 50 | 채널 id |
| `event.ts` | string | 조건부 | max: 50 | Slack timestamp |

**주의:** Slack payload의 사용자 표시명이나 email 문자열만으로 Notio 사용자를 매칭하지 않습니다.

**Error Cases:**
- `401 PROVIDER_SIGNATURE_INVALID`: Slack 서명 검증 실패
- `404 CONNECTION_NOT_FOUND`: `team_id`에 해당하는 active connection 없음
- `413 PAYLOAD_TOO_LARGE`: body 크기 초과
- `429 RATE_LIMIT_EXCEEDED`: 요청 횟수 제한 초과

### 6.4 Gmail Webhook

**인증 방식:** Google Pub/Sub/OIDC/subscription 검증 + `subscription_id` 또는 provider account id 기반 connection 매칭

**Request Body 주요 필드:**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| `message` | object | * | - | Pub/Sub message |
| `message.messageId` | string | * | max: 255 | Pub/Sub message id |
| `message.data` | string | * | base64 | Gmail notification payload |
| `message.attributes` | object | - | - | Pub/Sub attributes |
| `subscription` | string | * | max: 255 | subscription name |

**주의:** payload의 email 문자열만으로 Notio 사용자를 매칭하지 않습니다.

**Error Cases:**
- `401 PROVIDER_SIGNATURE_INVALID`: Google provider 검증 실패
- `404 CONNECTION_NOT_FOUND`: subscription/account에 해당하는 active connection 없음
- `413 PAYLOAD_TOO_LARGE`: body 크기 초과
- `429 RATE_LIMIT_EXCEEDED`: 요청 횟수 제한 초과

---

## 7. Notification API 변경사항

기존 Notification endpoint는 유지합니다.

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/{id}`
- `PATCH /api/v1/notifications/{id}/read`
- `PATCH /api/v1/notifications/read-all`
- `GET /api/v1/notifications/unread-count`
- `DELETE /api/v1/notifications/{id}`

### 7.1 사용자 격리 정책

- 모든 Notification API는 로그인 사용자 기준으로만 동작합니다.
- 목록 조회는 로그인 사용자의 알림만 반환합니다.
- 상세 조회는 로그인 사용자의 알림만 반환합니다.
- 읽음 처리는 로그인 사용자의 알림만 변경합니다.
- 전체 읽음 처리는 로그인 사용자의 알림만 변경합니다.
- 삭제는 로그인 사용자의 알림만 soft delete 합니다.
- unread count는 로그인 사용자의 미읽음 수만 반환합니다.
- 다른 사용자의 notification id에 접근하면 `404 NOTIFICATION_NOT_FOUND`를 반환합니다.

### 7.2 Notification 응답 필드 변경

기존 Notification 객체에 `connection_id`가 추가됩니다.

| 필드 | 타입 | Nullable | 제약사항 | 설명 |
|------|------|----------|----------|------|
| `connection_id` | int | Y | min: 1 | 알림을 생성한 connection ID |

**Response Example:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "connection_id": 13,
    "source": "CLAUDE",
    "title": "Claude Code 세션 종료",
    "body": "세션이 성공적으로 완료되었습니다",
    "priority": "MEDIUM",
    "is_read": false,
    "created_at": "2026-04-16T10:30:00Z",
    "updated_at": "2026-04-16T10:30:00Z",
    "external_id": "claude-notif-001",
    "external_url": "https://claude.ai/sessions/abc123",
    "metadata": {}
  },
  "error": null
}
```

---

## 8. Rate Limit

Rate limit은 Webhook뿐 아니라 모든 `/api/v1/**`에 적용합니다.

### 8.1 제외 경로

- `/swagger-ui/**`
- `/api-docs/**`
- health check
- static assets

### 8.2 정책

| 정책 | 식별자 | 제한 |
|------|--------|------|
| `auth-login` | IP | 5회/분, 30회/시간 |
| `auth-refresh` | IP | 30회/분 |
| `auth-refresh` | user | 60회/시간 |
| `webhook` | connection/key prefix | 30회/분, 5,000회/일 |
| `webhook` | IP | 60회/분 |
| `notifications-read` | user | 120회/분 |
| `notifications-write` | user | 60회/분 |
| `chat-ai` | user | 20회/분, 200회/일 |
| `device-register` | user | 10회/분 |
| `device-register` | IP | 30회/분 |

### 8.3 초과 응답

**Headers:**

| 헤더 | 필수 | 설명 |
|------|------|------|
| `Retry-After` | * | 재시도 가능 시간(초) |
| `X-RateLimit-Limit` | * | 적용된 제한 |
| `X-RateLimit-Remaining` | * | 남은 요청 수 |
| `X-RateLimit-Reset` | * | reset timestamp |

**Response 429 Too Many Requests:**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
  }
}
```

### 8.4 Payload 제한

| 대상 | 제한 | 초과 시 |
|------|------|---------|
| Webhook body | 64KB | 413 |
| 일반 JSON body | 1MB | 413 |
| Chat input | API validation 기준 | 400 |

Payload 제한 초과는 JSON parse 전에 거절해야 합니다.

---

## 9. 에러 코드

### 9.1 에러 코드 목록

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `RATE_LIMIT_EXCEEDED` | 429 | 요청 횟수 제한을 초과했습니다 |
| `PAYLOAD_TOO_LARGE` | 413 | 요청 본문 크기가 제한을 초과했습니다 |
| `CONNECTION_NOT_FOUND` | 404 | connection을 찾을 수 없습니다 |
| `CONNECTION_PROVIDER_UNSUPPORTED` | 400 | 지원하지 않는 provider입니다 |
| `CONNECTION_AUTH_TYPE_UNSUPPORTED` | 400 | 지원하지 않는 인증 방식입니다 |
| `CONNECTION_ALREADY_EXISTS` | 409 | 동일 범위 connection이 이미 존재합니다 |
| `CONNECTION_VERIFICATION_FAILED` | 401 | connection 검증에 실패했습니다 |
| `WEBHOOK_KEY_MISSING` | 401 | webhook API Key가 없습니다 |
| `WEBHOOK_KEY_INVALID` | 401 | webhook API Key가 유효하지 않습니다 |
| `WEBHOOK_KEY_EXPIRED` | 401 | webhook API Key가 만료되었습니다 |
| `WEBHOOK_KEY_REVOKED` | 401 | webhook API Key가 폐기되었습니다 |
| `WEBHOOK_SOURCE_MISMATCH` | 401 | webhook key provider와 요청 provider가 일치하지 않습니다 |
| `WEBHOOK_VERIFICATION_FAILED` | 401 | webhook 검증에 실패했습니다 |
| `OAUTH_STATE_INVALID` | 401 | OAuth state가 유효하지 않습니다 |
| `OAUTH_CALLBACK_FAILED` | 400 | OAuth callback 처리에 실패했습니다 |
| `PROVIDER_SIGNATURE_INVALID` | 401 | provider 서명 검증에 실패했습니다 |
| `NOTIFICATION_NOT_FOUND` | 404 | 알림을 찾을 수 없습니다 |
| `INVALID_REQUEST` | 400 | 잘못된 요청입니다 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류가 발생했습니다 |

### 9.2 에러 응답 예시

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "CONNECTION_NOT_FOUND",
    "message": "연동 정보를 찾을 수 없습니다."
  }
}
```

### 9.3 Webhook 에러 노출 정책

외부 webhook 요청에는 상세 실패 사유를 그대로 노출하지 않을 수 있습니다.

예:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "WEBHOOK_VERIFICATION_FAILED",
    "message": "Webhook 검증에 실패했습니다."
  }
}
```

상세 reason은 server log와 `connection_events.failure_reason`에 기록합니다.

---

## 10. 부록

### A. TypeScript 타입 예시

**Connection:**
```typescript
type Connection = {
  id: number;
  provider: "CLAUDE" | "SLACK" | "GMAIL" | "GITHUB" | "DISCORD" | "JIRA" | "LINEAR" | "TEAMS";
  auth_type: "API_KEY" | "OAUTH" | "SIGNATURE" | "SYSTEM";
  display_name: string;
  account_label: string | null;
  external_account_id?: string | null;
  external_workspace_id?: string | null;
  subscription_id?: string | null;
  status: "PENDING" | "ACTIVE" | "NEEDS_ACTION" | "REVOKED" | "ERROR";
  capabilities: Array<"WEBHOOK_RECEIVE" | "TEST_MESSAGE" | "REFRESH_TOKEN" | "ROTATE_KEY">;
  metadata?: Record<string, unknown>;
  key_preview: string | null;
  last_used_at: string | null;
  created_at: string;
  updated_at: string;
};
```

**ConnectionCreateRequest:**
```typescript
type ConnectionCreateRequest = {
  provider: Connection["provider"];
  auth_type: Connection["auth_type"];
  display_name: string;
};
```

**ConnectionCreateResponse:**
```typescript
type ConnectionCreateResponse = {
  connection: Pick<Connection, "id" | "provider" | "auth_type" | "display_name" | "status" | "key_preview">;
  api_key: string | null;
};
```

**ConnectionOAuthUrlResponse:**
```typescript
type ConnectionOAuthUrlResponse = {
  authorization_url: string;
  state_expires_in: number;
};
```

**ConnectionTestResponse:**
```typescript
type ConnectionTestResponse = {
  success: boolean;
  message: string;
  tested_at: string;
};
```

**WebhookReceiveResponse:**
```typescript
type WebhookReceiveResponse = {
  notification_id: number;
  connection_id: number;
  processed_at: string;
};
```

### B. API Key 형식

API Key는 JWT가 아니라 opaque token입니다.

```text
ntio_wh_<prefix>_<secret>
```

예:

```text
ntio_wh_ab12cd34_dGhpcy1pcy1hLXJhbmRvbS1zZWNyZXQ
```

저장 원칙:

- 원문 full key는 저장하지 않습니다.
- `key_hash = HMAC-SHA256(NOTIO_WEBHOOK_KEY_PEPPER, full_api_key)`만 저장합니다.
- `key_prefix`로 후보 credential을 조회합니다.
- hash 비교는 constant-time compare로 수행합니다.
- 원문 full key는 생성/rotate 응답에서만 1회 반환합니다.

### C. 환경변수

```bash
# Webhook API Key hashing
NOTIO_WEBHOOK_KEY_PEPPER=your-256-bit-random-pepper

# OAuth credential encryption
NOTIO_CREDENTIAL_ENCRYPTION_KEY=your-credential-encryption-key

# JWT
NOTIO_JWT_SECRET=your-256-bit-random-jwt-secret

# Redis
NOTIO_REDIS_HOST=localhost
NOTIO_REDIS_PORT=6379

# Claude hook script side
NOTIO_WEBHOOK_API_KEY=ntio_wh_ab12cd34_xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Deprecated:

```bash
NOTIO_WEBHOOK_CLAUDE_TOKEN=deprecated
```

### D. Validation 규칙 요약

| 필드 | 최소 | 최대 |
|------|------|------|
| Connection.display_name | 1 | 100 |
| Connection.account_label | - | 255 |
| Connection.external_account_id | - | 255 |
| Connection.external_workspace_id | - | 255 |
| Connection.subscription_id | - | 255 |
| Connection.key_preview | - | 80 |
| Notification.title | 1 | 255 |
| Notification.body | 1 | 2000 |
| Notification.external_id | - | 255 |
| Notification.external_url | - | 500 |
| Webhook Claude notification.message | 1 | 2000 |
| OAuth redirect_uri | - | 500 |
| page | 0 | - |
| size | 1 | 100 |

### E. 버전 히스토리

| 버전 | 날짜 | 변경사항 |
|------|------|----------|
| 1.4-fix | 2026-04-16 | Connections 기반 Webhook/Notification 보안 수정 명세 추가 |

---

**문서 끝**

이 문서는 Notio 프로젝트의 fix 범위 공식 API 명세서입니다.
Backend 개발 시 이 명세를 기준으로 구현하고, Frontend에서는 이 명세에 맞춰 API를 호출합니다.
API 변경 시 이 문서를 먼저 업데이트한 후 구현해야 합니다.

