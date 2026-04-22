# AI Chat RAG + LLM Phase 0 구축 계획

> 대상: Notio Phase 0 AI 채팅 기능을 더미 데이터 기반 응답에서 실제 RAG + LLM 기반 응답으로 전환  
> 기준: Spring Boot 모놀리스 유지, Docker Compose 기반 로컬 인프라, Phase 1 AI Service 분리 대비

---

## 1. 목표

현재 AI Chat 기능은 프론트엔드 API 연동과 화면 흐름은 구현되어 있지만, 백엔드는 더미/규칙 기반 응답을 반환한다.

이번 구축의 목표는 다음과 같다.

- 기존 Chat API 계약을 유지하면서 실제 LLM 응답으로 교체한다.
- 알림 데이터를 pgvector에 임베딩하여 RAG 검색에 사용한다.
- Docker Compose로 PostgreSQL, Redis, Ollama 기반 로컬 테스트 환경을 구성한다.
- 기존 `docker-compose/docker-compose.yml`의 PostgreSQL, Redis 설정은 변경하지 않는다.
- Phase 0에서는 Spring Boot 모놀리스 안에서 구현하되, Phase 1의 Python FastAPI/LangChain AI Service 분리를 쉽게 할 수 있도록 경계를 설계한다.

---

## 2. 현재 상태 요약

### 2.1 Frontend

프론트엔드는 AI Chat 화면과 API 연동 구조가 이미 준비되어 있다.

- `POST /api/v1/chat`
- `GET /api/v1/chat/stream`
- `GET /api/v1/chat/daily-summary`
- `GET /api/v1/chat/history`

따라서 기존 API 응답 형식과 SSE 이벤트 형식을 유지하면 프론트엔드 변경 없이 실제 RAG + LLM으로 전환할 수 있다.

### 2.2 Backend

현재 백엔드는 다음 상태다.

- `ChatService.chat(ChatRequest)`는 더미 응답을 생성한다.
- `ChatService.streamChat(ChatRequest)`는 더미 응답을 chunk로 쪼개 SSE로 전송한다.
- `DailySummaryService.getSummary()`는 알림 목록을 규칙 기반으로 요약한다.
- Spring AI Ollama 의존성은 아직 추가되어 있지 않다.
- `chat_messages` 엔티티/레포지토리는 아직 없다.
- `notification_embeddings` 같은 RAG 임베딩 테이블은 아직 없다.

### 2.3 Infra

현재 Docker Compose는 다음 상태다.

- PostgreSQL은 `ankane/pgvector:v0.5.1` 이미지로 설정되어 있다.
- Redis는 `redis:7-alpine`으로 설정되어 있다.
- Ollama 서비스는 주석 처리되어 있다.
- 기존 PostgreSQL, Redis 설정은 유지한다.

---

## 3. 구축 방향

### 3.1 선택한 전략

이번 구축은 **하이브리드 준비 전략**으로 진행한다.

- Phase 0: Spring Boot 모놀리스 내부에서 Spring AI + Ollama + pgvector로 실제 RAG + LLM 구현
- Phase 1: Python FastAPI + LangChain AI Service로 분리 가능하도록 인터페이스 경계 유지

### 3.2 핵심 원칙

- 프론트엔드 API 계약은 유지한다.
- 더미 응답 생성 로직은 제거한다.
- LLM, 임베딩, RAG 검색, 프롬프트 생성 책임은 명확히 분리한다.
- 스키마 변경은 Flyway SQL 파일로만 관리한다.
- pgvector 전용 쿼리는 JPA/QueryDSL에 억지로 통합하지 않고 native SQL 또는 `JdbcTemplate`으로 분리한다.

---

## 4. 최종 아키텍처

Phase 0 기준 아키텍처는 다음과 같다.

```text
Flutter App
  |
  | Chat API / SSE
  v
Spring Boot Monolith
  |
  | ChatController
  v
ChatService
  |
  +-- ChatMessageRepository
  |
  +-- RagRetriever
  |     |
  |     +-- PgvectorRagRetriever
  |
  +-- PromptBuilder
  |
  +-- LlmProvider
        |
        +-- OllamaLlmProvider

NotificationService
  |
  +-- NotificationEmbeddingService
        |
        +-- EmbeddingProvider
              |
              +-- OllamaEmbeddingProvider

PostgreSQL + pgvector
Redis
Ollama
```

### 4.1 주요 책임

#### ChatController

- 기존 Chat API 엔드포인트 유지
- 요청 검증
- `ChatService` 호출
- SSE 응답 전달

#### ChatService

- 사용자 메시지 저장
- 사용자 질문 임베딩 요청
- RAG 검색 요청
- 프롬프트 생성
- LLM 호출
- assistant 메시지 저장

#### DailySummaryService

- 오늘 알림 목록 조회
- LLM 기반 요약 생성
- Redis 24시간 캐시 적용
- LLM 장애 시 캐시가 있으면 캐시 응답 반환

#### NotificationEmbeddingService

- 알림 저장 후 임베딩 생성
- 임베딩 결과 저장
- 임베딩 실패 시 알림 저장은 롤백하지 않음

#### RagRetriever

- 사용자 질문 벡터와 알림 벡터 간 유사도 검색
- 기본 top-k는 5
- 검색 결과를 LLM prompt context로 전달

#### PromptBuilder

- system prompt, 사용자 질문, RAG context, 최근 채팅 히스토리 조립
- 응답 언어와 출력 제약 정의

#### LlmProvider

- LLM 단건 응답 생성
- LLM streaming 응답 생성
- Phase 1에서 remote AI Service client로 교체 가능한 경계 제공

#### EmbeddingProvider

- 텍스트 임베딩 생성
- Phase 1에서 AI Service의 embedding API로 교체 가능한 경계 제공

---

## 5. Docker Compose 인프라 설계

### 5.1 기본 원칙

기존 `docker-compose/docker-compose.yml`의 PostgreSQL, Redis 설정은 변경하지 않는다.

변경 대상은 다음으로 제한한다.

- 주석 처리된 `ollama` 서비스 활성화
- 주석 처리된 `ollama_data` 볼륨 활성화
- Ollama 모델 pull 절차 추가
- AI/RAG 환경변수 추가

### 5.2 Ollama 서비스

활성화할 Ollama 서비스는 다음과 같다.

```yaml
  ollama:
    image: ollama/ollama:latest
    container_name: notio-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    networks:
      - notio-network
```

볼륨은 다음 항목만 추가 활성화한다.

```yaml
volumes:
  postgres_data:
  redis_data:
  ollama_data:
```

### 5.3 실행 명령

repo root 기준으로 실행한다.

```bash
docker compose -f docker-compose/docker-compose.yml up -d postgres redis ollama
```

### 5.4 모델 다운로드

Ollama 컨테이너 기동 후 모델을 pull한다.

```bash
docker exec notio-ollama ollama pull llama3.2:3b
docker exec notio-ollama ollama pull nomic-embed-text
```

### 5.5 Backend 연결 기준

Spring Boot를 로컬에서 실행하는 경우:

```env
NOTIO_OLLAMA_URL=http://localhost:11434
NOTIO_DB_HOST=localhost
NOTIO_REDIS_HOST=localhost
```

Spring Boot를 나중에 Docker Compose 내부 서비스로 실행하는 경우:

```env
NOTIO_OLLAMA_URL=http://ollama:11434
NOTIO_DB_HOST=postgres
NOTIO_REDIS_HOST=redis
```

Phase 0에서는 backend 컨테이너 추가를 필수 범위로 두지 않는다.

---

## 6. 16GB RAM 기준 모델 선택

현재 로컬 PC RAM 기준은 16GB로 확정한다.

### 6.1 기본 추천 모델

| 용도 | 모델 | 선택 이유 |
|------|------|-----------|
| LLM | `llama3.2:3b` | 16GB RAM 테스트 환경에서 품질과 리소스 균형이 좋음 |
| Embedding | `nomic-embed-text` | Ollama에서 바로 사용 가능한 임베딩 전용 모델 |

### 6.2 대안 모델

| 용도 | 모델 | 사용 시점 |
|------|------|-----------|
| 더 가벼운 테스트 | `llama3.2:1b` | Docker, IDE, 브라우저 등으로 메모리 여유가 부족할 때 |
| 한국어/구조화 응답 비교 | `qwen2.5:1.5b` 또는 `qwen2.5:3b` | 한국어 응답 품질을 별도로 비교하고 싶을 때 |

### 6.3 기본 환경변수

`.env.example`과 backend 설정에는 다음 값을 추가한다.

```env
NOTIO_OLLAMA_URL=http://localhost:11434
NOTIO_LLM_MODEL=llama3.2:3b
NOTIO_EMBED_MODEL=nomic-embed-text
NOTIO_EMBED_DIM=768
NOTIO_RAG_TOP_K=5
```

---

## 7. Flyway + pgvector DB 설계

### 7.1 Flyway 사용 원칙

Flyway는 vectorDB 사용 자체의 필수 조건은 아니다.

하지만 Notio는 이미 Flyway로 DB 스키마를 관리하고 있고, 프로젝트 규칙상 스키마 변경은 Flyway로 관리해야 한다.

따라서 다음 작업은 모두 Flyway SQL 파일로 관리한다.

- `pgvector` extension 활성화
- `chat_messages` 테이블 생성
- `notification_embeddings` 테이블 생성
- vector index 생성

이미 적용된 기존 migration 파일은 수정하지 않고 새 버전 파일을 추가한다.

예:

```text
backend/src/main/resources/db/migration/V11__create_rag_embeddings.sql
```

실제 구현 시에는 현재 마지막 migration 번호 다음 번호를 사용한다.

### 7.2 pgvector extension

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 7.3 chat_messages

채팅 히스토리를 인메모리가 아니라 DB에 저장한다.

```sql
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_chat_messages_user_id_created_at
    ON chat_messages(user_id, created_at DESC)
    WHERE deleted_at IS NULL;
```

### 7.4 notification_embeddings

알림별 임베딩을 저장한다.

```sql
CREATE TABLE notification_embeddings (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source VARCHAR(50) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding vector(768) NOT NULL,
    embedded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_notification_embeddings_notification_id
    ON notification_embeddings(notification_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_notification_embeddings_user_id
    ON notification_embeddings(user_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_notification_embeddings_notification_hash
    ON notification_embeddings(notification_id, content_hash)
    WHERE deleted_at IS NULL;
```

### 7.5 vector index

초기 데이터가 적을 때는 full scan도 가능하지만, Phase 0부터 vector index를 준비한다.

```sql
CREATE INDEX idx_notification_embeddings_vector
    ON notification_embeddings
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
```

초기 데이터가 매우 적으면 `ivfflat`의 검색 품질이 기대와 다를 수 있으므로, 필요 시 초기 개발 중에는 index 없이 정확 검색으로 검증하고 이후 index를 적용한다.

---

## 8. JPA / QueryDSL / Native SQL 사용 방침

### 8.1 JPA

JPA는 일반 도메인 테이블에 사용한다.

대상:

- `notifications`
- `chat_messages`
- `todos`
- `devices`
- 일반적인 조회/저장/수정 로직

`vector(768)` 타입은 기본 JPA가 자연스럽게 다루기 어렵기 때문에 임베딩 저장/검색은 별도 repository로 분리한다.

### 8.2 QueryDSL

QueryDSL 사용 자체는 문제 없다.

적합한 영역:

- 알림 목록 필터링
- 읽음 여부 필터
- source 필터
- 날짜 범위 필터
- 일반 조인/정렬

부적합한 영역:

- `embedding <=> query_vector` 같은 pgvector 전용 연산자
- `vector(768)` 타입 직접 매핑

따라서 QueryDSL은 일반 조회에 사용하고, pgvector 유사도 검색은 native SQL로 분리한다.

### 8.3 Native SQL / JdbcTemplate

RAG 검색은 native SQL 또는 `JdbcTemplate` 기반 repository로 구현한다.

예:

```sql
SELECT
    notification_id,
    1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS score
FROM notification_embeddings
WHERE user_id = :userId
  AND deleted_at IS NULL
ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
LIMIT :topK;
```

이 방식은 Phase 0에서 가장 단순하고 안정적이며, Phase 1에서 Python AI Service로 옮기기도 쉽다.

---

## 9. Backend 구현 설계

### 9.1 패키지 구조

권장 구조:

```text
com.notio.chat
  ├── controller
  ├── domain
  ├── dto
  ├── repository
  ├── service
  ├── ai
  │   ├── LlmProvider.java
  │   ├── EmbeddingProvider.java
  │   ├── OllamaLlmProvider.java
  │   └── OllamaEmbeddingProvider.java
  ├── rag
  │   ├── RagRetriever.java
  │   ├── RagDocument.java
  │   └── PgvectorRagRetriever.java
  └── prompt
      └── PromptBuilder.java
```

임베딩 저장 책임은 notification 도메인과 연결되므로 다음 중 하나로 둔다.

```text
com.notio.notification.embedding
```

또는

```text
com.notio.chat.rag
```

Phase 0에서는 Chat RAG 기능에서만 사용하므로 `com.notio.chat.rag`에 두고, 이후 여러 도메인에서 공유하면 공통 AI/RAG 패키지로 승격한다.

### 9.2 핵심 인터페이스

#### LlmProvider

```java
public interface LlmProvider {
    String chat(LlmPrompt prompt);

    void stream(
            LlmPrompt prompt,
            java.util.function.Consumer<String> onChunk,
            Runnable onComplete
    );
}
```

#### EmbeddingProvider

```java
public interface EmbeddingProvider {
    float[] embed(String text);
}
```

#### RagRetriever

```java
public interface RagRetriever {
    List<RagDocument> retrieve(Long userId, String query, int topK);
}
```

#### PromptBuilder

```java
public interface PromptBuilder {
    LlmPrompt buildChatPrompt(
            String userMessage,
            List<RagDocument> documents,
            List<ChatMessage> recentMessages
    );
}
```

### 9.3 ChatService 동작 흐름

단건 응답:

```text
1. user message 저장
2. query embedding 생성
3. pgvector top-k 검색
4. prompt 생성
5. Ollama LLM 호출
6. assistant message 저장
7. ChatMessageResponse 반환
```

스트리밍 응답:

```text
1. user message 저장
2. query embedding 생성
3. pgvector top-k 검색
4. prompt 생성
5. Ollama stream 호출
6. chunk를 SSE로 전달
7. 전체 assistant content 누적
8. stream 완료 후 assistant message 저장
9. done 이벤트 전송
```

### 9.4 Notification embedding 흐름

```text
1. webhook 또는 내부 이벤트로 notification 저장
2. title + body + metadata 일부를 embedding input으로 구성
3. content_hash 생성
4. 같은 notification_id + content_hash가 있으면 skip
5. Ollama embedding 호출
6. notification_embeddings 저장
7. 실패 시 로그 기록 후 알림 저장은 성공 처리
```

### 9.5 DailySummaryService 흐름

```text
1. userId + 오늘 날짜 기준 Redis cache 확인
2. cache hit이면 반환
3. 오늘 알림 목록 조회
4. PromptBuilder로 daily summary prompt 생성
5. LlmProvider 호출
6. topics 추출
7. Redis에 24시간 캐시
8. DailySummaryResponse 반환
```

---

## 10. Prompt 설계

### 10.1 기본 원칙

- 기본 응답 언어는 한국어다.
- 알림 context에 없는 사실은 단정하지 않는다.
- 중요한 알림을 말할 때 source, title, priority 근거를 함께 제공한다.
- 너무 긴 답변은 피하고, Chat API의 `content` 최대 길이 4000자를 지킨다.
- RAG context가 없으면 "현재 검색 가능한 관련 알림이 없다"는 fallback 응답을 제공한다.

### 10.2 Chat prompt 구성

```text
System:
너는 개발자를 위한 알림 관리 AI 어시스턴트다.
사용자의 알림 데이터를 근거로 요약, 우선순위 판단, 할일 후보 제안을 도와라.
제공된 컨텍스트에 없는 사실은 추측하지 마라.
응답은 한국어로 작성하라.

Context:
- source
- title
- body summary
- priority
- created_at
- similarity score

Recent conversation:
- 최근 user/assistant 메시지 일부

User:
{사용자 질문}
```

### 10.3 Daily summary prompt 구성

```text
오늘 수집된 알림 목록을 바탕으로 다음을 요약하라.

1. 전체 요약
2. 중요한 알림
3. 사용자가 바로 처리하면 좋은 항목
4. 주요 topic

응답은 간결한 한국어로 작성하라.
```

---

## 11. API 호환성 유지 전략

프론트엔드 변경을 최소화하기 위해 기존 API를 유지한다.

### 11.1 유지할 엔드포인트

- `POST /api/v1/chat`
- `GET /api/v1/chat/stream`
- `GET /api/v1/chat/daily-summary`
- `GET /api/v1/chat/history`

### 11.2 유지할 응답 구조

`POST /api/v1/chat`은 기존처럼 assistant 메시지를 반환한다.

```json
{
  "success": true,
  "data": {
    "id": 123,
    "role": "ASSISTANT",
    "content": "응답 내용",
    "created_at": "2026-04-22T10:00:00Z"
  },
  "error": null
}
```

SSE는 기존 프론트가 처리 가능한 `chunk`, `done` 흐름을 유지한다.

```text
data: {"chunk":"오늘"}

data: {"chunk":" 중요한 알림은"}

data: {"done":true,"message_id":124}
```

---

## 12. 장애 처리 정책

### 12.1 LLM 장애

Ollama 연결 실패, timeout, 모델 미설치 등의 경우:

- Chat API: `LLM_UNAVAILABLE`
- Daily summary: 캐시가 있으면 캐시 반환, 없으면 `LLM_UNAVAILABLE`
- 로그에는 모델명, 요청 id, 오류 메시지만 남기고 민감 데이터는 남기지 않는다.

### 12.2 Embedding 장애

임베딩 생성 실패 시:

- 알림 저장은 성공 처리한다.
- `EMBEDDING_FAILED` 로그를 남긴다.
- 재처리 가능한 구조를 남긴다.

Phase 0에서는 별도 queue 없이 동기 처리 후 실패 로그를 남긴다.
Phase 1에서는 Celery 또는 별도 async worker로 이전한다.

### 12.3 RAG 검색 결과 없음

RAG 검색 결과가 없으면 다음 fallback을 사용한다.

- 최근 알림 목록 기반 일반 요약
- 또는 "관련 알림을 찾지 못했다"는 안내 응답

---

## 13. 구현 단계

### Step 1. Docker Compose Ollama 활성화

- 기존 PostgreSQL, Redis 설정은 변경하지 않는다.
- 주석 처리된 `ollama` 서비스를 활성화한다.
- `ollama_data` 볼륨을 활성화한다.
- 모델 pull 명령을 문서화하거나 setup script에 반영한다.

### Step 2. 환경변수 추가

- `.env.example`에 AI/RAG 환경변수 추가
- `application.yml`에 Spring AI/Ollama, Notio RAG 설정 바인딩 추가

### Step 3. Spring AI/Ollama 의존성 추가

- Spring AI Ollama 의존성 추가
- `SpringAiConfig` 또는 동등한 설정 클래스 추가
- timeout, model, base URL 설정 가능하게 구성

### Step 4. Flyway migration 추가

- `CREATE EXTENSION IF NOT EXISTS vector`
- `chat_messages`
- `notification_embeddings`
- pgvector index

### Step 5. ChatMessage 영속화

- `ChatMessage` entity 추가
- `ChatMessageRole` enum 추가
- `ChatMessageRepository` 추가
- 기존 인메모리 `history` 제거

### Step 6. Embedding pipeline 구현

- `EmbeddingProvider` 추가
- `OllamaEmbeddingProvider` 구현
- `NotificationEmbeddingRepository`를 native SQL/JdbcTemplate 기반으로 구현
- 알림 저장 후 임베딩 생성 연동

### Step 7. RAG 검색 구현

- `RagRetriever` 추가
- `PgvectorRagRetriever` 구현
- 질문 임베딩 생성 후 top-k 검색

### Step 8. PromptBuilder 구현

- Chat prompt
- Daily summary prompt
- Todo title generation prompt는 후속 단계에서 추가

### Step 9. ChatService 더미 제거

- `generateDummyAiResponse` 제거
- 실제 RAG + LLM 호출로 교체
- SSE는 실제 LLM stream chunk 전달로 교체

### Step 10. DailySummaryService LLM 전환

- 규칙 기반 summary를 LLM 기반 summary로 교체
- Redis 캐시 유지
- LLM 장애 fallback 적용

### Step 11. Todo/Analytics 확장

Chat 안정화 이후 다음 기능에 같은 provider를 적용한다.

- `TodoService.createFromNotification`의 LLM 제목 생성
- `AnalyticsService.getWeeklySummary`의 LLM insight 생성

---

## 14. 테스트 계획

### 14.1 Unit Test

- `PromptBuilderTest`
- `ChatServiceTest`
- `DailySummaryServiceTest`
- `NotificationEmbeddingServiceTest`
- `RagRetrieverTest`

검증 항목:

- RAG context가 prompt에 포함되는지
- 검색 결과가 없을 때 fallback이 동작하는지
- LLM 장애가 `LLM_UNAVAILABLE`로 매핑되는지
- 임베딩 장애가 알림 저장을 롤백하지 않는지
- daily summary cache key가 user/date 기준으로 분리되는지

### 14.2 Repository Test

- pgvector extension 활성화 확인
- embedding insert 확인
- cosine similarity search 확인
- user scope isolation 확인

### 14.3 Controller Test

- `POST /api/v1/chat` 응답 형식 유지
- `GET /api/v1/chat/history` 응답 형식 유지
- `GET /api/v1/chat/daily-summary` 응답 형식 유지
- SSE chunk/done 이벤트 유지

### 14.4 Local Smoke Test

인프라 확인:

```bash
docker compose -f docker-compose/docker-compose.yml ps
docker exec notio-ollama ollama list
curl http://localhost:11434/api/tags
```

Backend 확인:

```bash
cd backend
./gradlew test
./gradlew bootRun
```

Chat API 확인:

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"content":"오늘 중요한 알림 요약해줘"}'
```

---

## 15. Phase 1 AI Service 분리 대비

Phase 1에서는 Python FastAPI + LangChain 기반 AI Service로 분리한다.

Phase 0에서 다음 경계를 유지하면 분리가 쉽다.

- `LlmProvider`
- `EmbeddingProvider`
- `RagRetriever`
- `PromptBuilder`

Phase 1 전환 후 구조:

```text
Spring Boot ChatService
  |
  +-- AiServiceClient
        |
        +-- Python FastAPI AI Service
              |
              +-- LangChain
              +-- Ollama or other LLM Provider
              +-- pgvector retrieval
```

Spring Boot는 기존 Chat API를 유지하고 내부 provider 구현만 local Ollama에서 remote AI Service client로 교체한다.

---

## 16. 확정된 기본값

| 항목 | 값 |
|------|-----|
| 구축 전략 | Phase 0 모놀리스 구현 + Phase 1 분리 대비 |
| LLM runtime | Ollama |
| 기본 LLM model | `llama3.2:3b` |
| 기본 embedding model | `nomic-embed-text` |
| embedding dimension | `768` |
| RAG top-k | `5` |
| Vector DB | PostgreSQL + pgvector |
| Cache | Redis |
| DB 변경 관리 | Flyway SQL |
| 일반 ORM | Spring Data JPA |
| 일반 동적 쿼리 | QueryDSL 사용 가능 |
| pgvector 검색 | Native SQL 또는 JdbcTemplate |
| Frontend 변경 | 기존 API 계약 유지 시 불필요 |
| Docker Compose 변경 범위 | Ollama 활성화만, 기존 DB/Redis 설정 유지 |

---

## 17. 제외 범위

이번 Phase 0 RAG + LLM 구축에서 제외하는 항목은 다음과 같다.

- Python FastAPI AI Service 신규 생성
- LangChain 도입
- Celery worker 도입
- Kafka 기반 비동기 이벤트 처리
- 클라우드 LLM fallback
- GPU 전용 Docker Compose override
- 프론트엔드 UI 대규모 변경
- Backend 컨테이너화

이 항목들은 Phase 1 또는 별도 개선 단계에서 진행한다.
