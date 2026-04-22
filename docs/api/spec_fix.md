# Notio Fix API 명세서

**버전**: v3.0-ai-chat-rag  
**Base URL**: `http://localhost:8080`  
**API Version**: `/api/v1`  
**최종 수정일**: 2026-04-22

---

## 중요 사항

이 문서는 `docs/plans/plan_fix.md` 기준의 AI Chat RAG + LLM Phase 0 전환 API 명세서다.

핵심 원칙은 다음과 같다.

- 기존 Chat API endpoint를 변경하지 않는다.
- 기존 `ApiResponse` wrapper를 유지한다.
- JSON 필드는 `snake_case`를 사용한다.
- 프론트가 이미 사용하는 request/response 구조를 유지한다.
- 더미 응답만 실제 RAG + LLM 응답으로 교체한다.
- SSE는 기존 프론트가 처리 가능한 `chunk`, `done` 흐름을 유지한다.

---

## 목차

1. [공통 사항](#1-공통-사항)
2. [Chat 공통 타입](#2-chat-공통-타입)
3. [Chat API](#3-chat-api)
4. [SSE Streaming](#4-sse-streaming)
5. [Daily Summary API](#5-daily-summary-api)
6. [History API](#6-history-api)
7. [에러 코드](#7-에러-코드)
8. [운영 및 인프라 기준](#8-운영-및-인프라-기준)

---

## 1. 공통 사항

### 1.1 공통 응답 형식

성공 응답:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

실패 응답:

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

### 1.2 상태 코드

| 상태 코드 | 설명 |
|-----------|------|
| `200 OK` | 일반 성공 응답 |
| `400 Bad Request` | 요청 형식 또는 값 검증 실패 |
| `401 Unauthorized` | 인증 실패 또는 토큰 오류 |
| `403 Forbidden` | 접근 권한 없음 |
| `404 Not Found` | 리소스 없음 |
| `429 Too Many Requests` | Rate limit 초과 |
| `500 Internal Server Error` | 서버 내부 오류 |
| `503 Service Unavailable` | LLM/Ollama 사용 불가 |

### 1.3 필드 명명 규칙

- JSON 필드는 `snake_case`를 사용한다.
- enum 문자열은 `UPPER_SNAKE_CASE`를 사용한다.
- 날짜/시간은 ISO 8601 문자열을 사용한다.
- Chat 응답의 시간 필드는 `created_at`이다.
- Daily Summary 응답의 총 알림 수 필드는 `total_messages`이다.

### 1.4 인증 기준

Phase 0의 기존 API 계약은 유지한다.

인증이 활성화된 환경에서는 다음 header를 사용한다.

```http
Authorization: Bearer {access_token}
```

로컬 Phase 0 개발 중 legacy default user scope를 사용하는 경우에도 API 계약은 변경하지 않는다.

---

## 2. Chat 공통 타입

### 2.1 ChatRequest

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `content` | string | Y | blank 불가 | 사용자 메시지 |

예시:

```json
{
  "content": "오늘 중요한 알림 요약해줘"
}
```

### 2.2 ChatMessage

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | number | Y | 채팅 메시지 ID |
| `role` | string | Y | 메시지 역할 |
| `content` | string | Y | 메시지 본문 |
| `created_at` | string | Y | 생성 시각, ISO 8601 |

예시:

```json
{
  "id": 123,
  "role": "ASSISTANT",
  "content": "오늘 중요한 알림은 GitHub PR 리뷰 요청입니다.",
  "created_at": "2026-04-22T10:00:00Z"
}
```

### 2.3 MessageRole

| 값 | 설명 |
|----|------|
| `USER` | 사용자 메시지 |
| `ASSISTANT` | AI assistant 메시지 |

### 2.4 DailySummary

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `summary` | string | Y | 오늘 알림 요약 |
| `date` | string | Y | 요약 기준 날짜, `YYYY-MM-DD` |
| `total_messages` | number | Y | 요약 대상 알림 수 |
| `topics` | string[] | Y | 주요 topic 목록 |

예시:

```json
{
  "summary": "오늘은 GitHub 리뷰 요청과 Slack 장애 대응 메시지가 중요합니다.",
  "date": "2026-04-22",
  "total_messages": 12,
  "topics": ["GitHub", "Slack", "장애 대응"]
}
```

---

## 3. Chat API

### 3.1 메시지 전송

**Endpoint**

```http
POST /api/v1/chat
```

**설명**

- 사용자 메시지를 저장한다.
- 사용자 메시지를 embedding으로 변환한다.
- pgvector에서 관련 알림 top-k를 검색한다.
- 검색 결과와 최근 대화 히스토리로 prompt를 구성한다.
- Ollama LLM을 호출한다.
- assistant 메시지를 저장하고 반환한다.

**Request Body**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `content` | string | Y | blank 불가 | 사용자 메시지 |

**예시 요청**

```json
{
  "content": "오늘 처리해야 할 중요한 알림을 알려줘"
}
```

**성공 응답**

상태 코드: `200 OK`

```json
{
  "success": true,
  "data": {
    "id": 124,
    "role": "ASSISTANT",
    "content": "오늘 우선 처리할 알림은 GitHub PR 리뷰 요청입니다. priority가 HIGH이고 최근에 생성되었습니다.",
    "created_at": "2026-04-22T10:01:00Z"
  },
  "error": null
}
```

**동작 규칙**

- 응답 `role`은 항상 `ASSISTANT`이다.
- `content`는 RAG context를 근거로 생성한다.
- 관련 알림이 없으면 관련 알림이 없다는 fallback 응답을 반환한다.
- LLM이 사용 불가하면 표준 error response를 반환한다.

**에러 케이스**

| 상태 코드 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `400` | `INVALID_INPUT_VALUE` | `content`가 비어 있거나 형식이 잘못됨 |
| `503` | `LLM_UNAVAILABLE` | Ollama 연결 실패, timeout, 모델 미설치 |
| `500` | `EMBEDDING_FAILED` | 질문 임베딩 생성 실패 |
| `500` | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

## 4. SSE Streaming

### 4.1 메시지 스트리밍

**Endpoint**

```http
GET /api/v1/chat/stream
```

**설명**

- 기존 프론트의 SSE 연동을 유지한다.
- query parameter로 사용자 메시지를 전달한다.
- 실제 LLM streaming chunk를 SSE로 전달한다.
- stream 완료 후 assistant 메시지를 저장한다.

**Query Parameters**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `content` | string | Y | blank 불가 | 사용자 메시지 |

**예시 요청**

```http
GET /api/v1/chat/stream?content=오늘%20중요한%20알림%20요약해줘
Accept: text/event-stream
```

**성공 응답**

상태 코드: `200 OK`  
Content-Type: `text/event-stream`

```text
data: {"chunk":"오늘"}

data: {"chunk":" 중요한 알림은"}

data: {"chunk":" GitHub PR 리뷰 요청입니다."}

data: {"done":true,"message_id":125}
```

**SSE Chunk Event**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `chunk` | string | Y | assistant 응답 일부 |

**SSE Done Event**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `done` | boolean | Y | 완료 여부, 항상 `true` |
| `message_id` | number | Y | 저장된 assistant 메시지 ID |

**동작 규칙**

- event name을 별도로 요구하지 않는다.
- 모든 payload는 `data: ` prefix로 전송한다.
- 완료 전까지 assistant content를 서버에서 누적한다.
- 완료 후 누적 content를 `chat_messages`에 저장한다.
- client disconnect 시 서버 리소스를 정리한다.

**에러 케이스**

| 상태 코드 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `400` | `INVALID_INPUT_VALUE` | `content`가 비어 있음 |
| `503` | `LLM_UNAVAILABLE` | Ollama 연결 실패, timeout, 모델 미설치 |
| `500` | `EMBEDDING_FAILED` | 질문 임베딩 생성 실패 |
| `500` | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

## 5. Daily Summary API

### 5.1 오늘 요약 조회

**Endpoint**

```http
GET /api/v1/chat/daily-summary
```

**설명**

- 사용자와 오늘 날짜 기준으로 Redis cache를 먼저 조회한다.
- cache miss이면 오늘 알림 목록을 조회한다.
- LLM으로 오늘 요약을 생성한다.
- Redis에 24시간 캐시한다.
- 기존 `DailySummary` 응답 구조를 유지한다.

**성공 응답**

상태 코드: `200 OK`

```json
{
  "success": true,
  "data": {
    "summary": "오늘은 GitHub 리뷰 요청 2건과 Slack 장애 대응 메시지가 주요 이슈입니다.",
    "date": "2026-04-22",
    "total_messages": 8,
    "topics": ["GitHub", "Slack", "리뷰", "장애 대응"]
  },
  "error": null
}
```

**동작 규칙**

- cache hit이면 LLM을 호출하지 않는다.
- cache miss이면 LLM 기반 요약을 생성한다.
- LLM 장애 시 기존 cache가 있으면 cache 응답을 반환한다.
- LLM 장애 시 cache가 없으면 `LLM_UNAVAILABLE`을 반환한다.
- 알림이 없으면 빈 상태 요약과 `total_messages=0`을 반환한다.

**에러 케이스**

| 상태 코드 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `503` | `LLM_UNAVAILABLE` | cache miss 상태에서 LLM 사용 불가 |
| `500` | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

## 6. History API

### 6.1 채팅 히스토리 조회

**Endpoint**

```http
GET /api/v1/chat/history
```

**설명**

- 현재 사용자의 채팅 히스토리를 조회한다.
- 기존 프론트 계약을 유지하기 위해 list 형태의 `data`를 반환한다.
- Phase 0에서는 query parameter가 전달되더라도 기존 endpoint 계약을 깨지 않는다.

**Query Parameters**

| 필드 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| `page` | number | N | `0` | 프론트 호환용. 실제 적용 여부는 구현 단계에서 결정 |
| `size` | number | N | `20` | 프론트 호환용. 실제 적용 여부는 구현 단계에서 결정 |

**성공 응답**

상태 코드: `200 OK`

```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "role": "USER",
      "content": "오늘 중요한 알림 알려줘",
      "created_at": "2026-04-22T09:59:50Z"
    },
    {
      "id": 102,
      "role": "ASSISTANT",
      "content": "오늘 중요한 알림은 GitHub PR 리뷰 요청입니다.",
      "created_at": "2026-04-22T10:00:00Z"
    }
  ],
  "error": null
}
```

**동작 규칙**

- 삭제되지 않은 메시지만 반환한다.
- 사용자 scope를 반드시 적용한다.
- 정렬은 최신순 또는 기존 프론트 표시 흐름과 호환되는 순서로 고정한다.
- 응답 wrapper는 page object가 아니라 list를 유지한다.

**에러 케이스**

| 상태 코드 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `500` | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

## 7. 에러 코드

| 에러 코드 | HTTP 상태 | 설명 |
|-----------|-----------|------|
| `INVALID_INPUT_VALUE` | `400` | request body 또는 query parameter 검증 실패 |
| `UNAUTHORIZED` | `401` | 인증 실패 또는 access token 누락 |
| `FORBIDDEN` | `403` | 접근 권한 없음 |
| `LLM_UNAVAILABLE` | `503` | Ollama, LLM model, streaming 연결 사용 불가 |
| `EMBEDDING_FAILED` | `500` | embedding 생성 실패 |
| `INTERNAL_SERVER_ERROR` | `500` | 서버 내부 오류 |

### 7.1 LLM_UNAVAILABLE 예시

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "LLM_UNAVAILABLE",
    "message": "AI 응답을 생성할 수 없습니다. 잠시 후 다시 시도해주세요."
  }
}
```

### 7.2 INVALID_INPUT_VALUE 예시

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_INPUT_VALUE",
    "message": "메시지 내용을 입력해주세요."
  }
}
```

---

## 8. 운영 및 인프라 기준

### 8.1 기본 환경 변수

| 환경 변수 | 기본값 | 설명 |
|-----------|--------|------|
| `NOTIO_OLLAMA_URL` | `http://localhost:11434` | 로컬 Ollama endpoint |
| `NOTIO_LLM_MODEL` | `llama3.2:3b` | 기본 LLM 모델 |
| `NOTIO_EMBED_MODEL` | `nomic-embed-text` | 기본 embedding 모델 |
| `NOTIO_EMBED_DIM` | `768` | embedding dimension |
| `NOTIO_RAG_TOP_K` | `5` | RAG 검색 결과 개수 |

### 8.2 Docker Compose 기준

- 기존 PostgreSQL 설정은 변경하지 않는다.
- 기존 Redis 설정은 변경하지 않는다.
- Ollama 서비스만 활성화한다.
- `ollama_data` 볼륨을 활성화한다.

### 8.3 모델 기준

| 용도 | 모델 |
|------|------|
| LLM | `llama3.2:3b` |
| Embedding | `nomic-embed-text` |

### 8.4 RAG 기준

- Vector DB는 PostgreSQL + pgvector를 사용한다.
- embedding 컬럼은 `vector(768)`을 사용한다.
- RAG 검색 기본 top-k는 `5`다.
- pgvector 검색은 native SQL 또는 `JdbcTemplate`으로 구현한다.
- JPA/QueryDSL은 일반 도메인 조회에 사용한다.

### 8.5 호환성 검증 기준

- `POST /api/v1/chat` 응답은 `ChatMessage` 단일 객체다.
- `GET /api/v1/chat/stream`은 `text/event-stream`이다.
- `GET /api/v1/chat/daily-summary` 응답은 `DailySummary` 단일 객체다.
- `GET /api/v1/chat/history` 응답은 `ChatMessage[]` list다.
- 모든 JSON 필드는 `snake_case`다.
