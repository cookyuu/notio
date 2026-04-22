# Backend Fix 개발 체크리스트

> 대상: AI Chat 더미 응답을 실제 RAG + LLM 기반 응답으로 전환  
> 범위: Spring Boot 4.x · Java 25 · PostgreSQL pgvector · Redis · Ollama · Flyway

---

## Phase 0. 범위 확정 및 현재 계약 검증

- [x] `docs/plans/plan_fix.md`를 기준 문서로 확정한다.
- [x] Phase 0에서는 Spring Boot 모놀리스 내부에서 RAG + LLM을 구현한다.
- [x] Phase 1의 Python FastAPI/LangChain AI Service 분리를 고려해 `LlmProvider`, `EmbeddingProvider`, `RagRetriever`, `PromptBuilder` 경계를 둔다.
- [x] 기존 Chat API 엔드포인트를 유지한다.
- [x] 기존 `ApiResponse` 응답 형식을 유지한다.
- [x] SSE 이벤트 흐름은 기존 프론트가 처리 가능한 `chunk`, `done` 구조로 유지한다.
- [x] 기존 `docker-compose/docker-compose.yml`의 PostgreSQL, Redis 설정은 변경하지 않는다.
- [x] DB 스키마 변경은 Flyway 신규 migration 파일로만 관리한다.

### Phase 0 확인 메모

- 유지 대상 API는 `POST /api/v1/chat`, `GET /api/v1/chat/stream`, `GET /api/v1/chat/daily-summary`, `GET /api/v1/chat/history`이다.
- 현재 `ChatService`는 인메모리 history와 더미/규칙 기반 응답을 사용하므로 실제 구현 단계에서 제거 대상이다.
- 현재 `DailySummaryService`는 규칙 기반 요약이므로 LLM 기반 요약으로 전환한다.
- 현재 프론트는 `created_at`, `total_messages` 같은 `snake_case` JSON 필드를 기대하며, 백엔드 Jackson 설정도 `SNAKE_CASE`를 사용한다.

### Phase 0 검증 결과

- 기준 문서는 `docs/plans/plan_fix.md`로 확정하고, API 계약은 `docs/api/spec_fix.md`를 따른다.
- 현재 유지 대상 엔드포인트는 `ChatController`에 모두 존재한다: `POST /api/v1/chat`, `GET /api/v1/chat/stream`, `GET /api/v1/chat/daily-summary`, `GET /api/v1/chat/history`.
- 현재 공통 응답은 `ApiResponse<T>`의 `success`, `data`, `error` 구조를 사용한다.
- 현재 백엔드 Jackson 설정은 `SNAKE_CASE`이며, `ChatMessageResponse.createdAt`과 `DailySummaryResponse.totalMessages`는 각각 `created_at`, `total_messages`로 직렬화된다.
- 현재 Docker Compose의 PostgreSQL, Redis 설정은 유지되어 있고 Ollama는 아직 주석 처리 상태다.
- 현재 DB schema 변경은 `backend/src/main/resources/db/migration`의 Flyway migration으로 관리되고 있다.
- 현재 SSE 구현은 event name `chunk`, `done`과 raw data를 사용한다. `spec_fix.md`의 JSON payload 예시(`{"chunk": ...}`, `{"done": true, "message_id": ...}`)와는 차이가 있으므로 Phase 10 구현 시 프론트 파서와 함께 최종 정합성을 맞춘다.
- 현재 Chat role 응답은 프론트 기존 파서 기준 `user`, `assistant` 소문자 문자열이다. `spec_fix.md`의 `USER`, `ASSISTANT` 표기와 차이가 있으므로 실제 영속화 전환 단계에서 계약을 재확인한다.

## Phase 1. Docker Compose 인프라 준비

- [x] 기존 PostgreSQL 서비스 설정을 변경하지 않는다.
- [x] 기존 Redis 서비스 설정을 변경하지 않는다.
- [x] 주석 처리된 `ollama` 서비스를 활성화한다.
- [x] `ollama_data` 볼륨을 활성화한다.
- [x] `ollama` 서비스는 기존 `notio-network`에 연결한다.
- [x] 로컬 실행 기준 `11434:11434` 포트를 노출한다.
- [x] `scripts/setup.sh` 또는 별도 문서에 모델 pull 절차를 반영한다.
- [x] `llama3.2:3b` 모델 pull 절차를 검증한다.
- [x] `nomic-embed-text` 모델 pull 절차를 검증한다.

### Phase 1 확인 메모

- 16GB RAM 로컬 테스트 기준 기본 LLM은 `llama3.2:3b`로 둔다.
- 기본 embedding 모델은 `nomic-embed-text`로 둔다.
- 메모리 부족 시 `llama3.2:1b`를 임시 대안으로 사용할 수 있지만 기본값은 변경하지 않는다.
- `docker-compose/docker-compose.yml`에서 PostgreSQL/Redis 설정은 그대로 유지하고 Ollama 서비스와 `ollama_data` 볼륨만 활성화했다.
- `scripts/setup.sh`는 루트 실행 기준 `docker compose -f docker-compose/docker-compose.yml up -d postgres redis ollama`를 사용하고, `llama3.2:3b`, `nomic-embed-text` pull 명령을 포함한다.
- 검증: `bash -n scripts/setup.sh` 통과. 현재 WSL 환경에는 Docker CLI가 없어 `docker compose config` 및 실제 `ollama pull` 실행은 수행하지 못했다.

## Phase 2. 환경 변수 및 설정 추가

- [ ] `.env.example`에 `NOTIO_` prefix를 지키는 AI/RAG 환경 변수를 추가한다.
- [ ] `NOTIO_OLLAMA_URL` 기본값을 로컬 실행 기준 `http://localhost:11434`로 문서화한다.
- [ ] `NOTIO_LLM_MODEL` 기본값을 `llama3.2:3b`로 둔다.
- [ ] `NOTIO_EMBED_MODEL` 기본값을 `nomic-embed-text`로 둔다.
- [ ] `NOTIO_EMBED_DIM` 기본값을 `768`로 둔다.
- [ ] `NOTIO_RAG_TOP_K` 기본값을 `5`로 둔다.
- [ ] `application.yml`에 Spring AI/Ollama 연결 설정을 추가한다.
- [ ] `application.yml`에 Notio RAG 설정 바인딩을 추가한다.
- [ ] timeout, retry, streaming timeout 값을 환경별로 조정 가능하게 둔다.

### Phase 2 확인 메모

- Docker 내부에서 backend를 실행하는 후속 단계에서는 `NOTIO_OLLAMA_URL=http://ollama:11434`를 사용한다.
- Phase 0에서는 backend 컨테이너 추가를 필수 범위로 두지 않는다.

## Phase 3. Flyway + pgvector 스키마 구축

- [ ] 현재 마지막 Flyway migration 번호를 확인한다.
- [ ] 이미 적용된 기존 migration 파일은 수정하지 않는다.
- [ ] 신규 migration 파일을 추가한다.
- [ ] `CREATE EXTENSION IF NOT EXISTS vector`를 추가한다.
- [ ] `chat_messages` 테이블을 생성한다.
- [ ] `chat_messages`에 `id`, `user_id`, `role`, `content`, `created_at`, `updated_at`, `deleted_at` 컬럼을 둔다.
- [ ] `idx_chat_messages_user_id_created_at` 인덱스를 생성한다.
- [ ] `notification_embeddings` 테이블을 생성한다.
- [ ] `notification_embeddings.embedding` 컬럼은 `vector(768)`로 생성한다.
- [ ] `notification_embeddings`에 `notification_id`, `user_id`, `source`, `content_hash`, `embedded_at`, timestamp, soft delete 컬럼을 둔다.
- [ ] `notification_id + content_hash` unique partial index를 생성한다.
- [ ] user scope 검색용 인덱스를 생성한다.
- [ ] pgvector 유사도 검색용 index 적용 여부를 데이터 규모 기준으로 검증한다.

### Phase 3 확인 메모

- pgvector 사용 자체에 Flyway가 기술적으로 필수는 아니지만, Notio 프로젝트 규칙상 DB 변경은 Flyway로만 관리한다.
- 초기 데이터가 적으면 `ivfflat` index 품질이 기대와 다를 수 있으므로 정확 검색 검증 후 index 적용을 판단한다.

## Phase 4. Backend 의존성 및 설정 클래스 추가

- [ ] Spring AI Ollama 의존성을 추가한다.
- [ ] Spring AI 버전과 Spring Boot 4.0.0 호환성을 확인한다.
- [ ] Ollama chat model 설정을 구성한다.
- [ ] Ollama embedding model 설정을 구성한다.
- [ ] `SpringAiConfig` 또는 동등한 설정 클래스를 추가한다.
- [ ] model name, base URL, timeout을 환경 변수로 주입한다.
- [ ] LLM 연결 실패를 표준 예외로 변환하는 공통 처리 지점을 둔다.

### Phase 4 확인 메모

- 새 프레임워크를 추가하지 않고 Spring AI + Ollama 범위로 제한한다.
- Phase 1에서 provider 구현만 remote AI Service client로 교체할 수 있어야 한다.

## Phase 5. ChatMessage 영속화

- [ ] `ChatMessage` entity를 추가한다.
- [ ] `ChatMessageRole` enum을 추가한다.
- [ ] role 값은 `USER`, `ASSISTANT`를 기본으로 둔다.
- [ ] `ChatMessageRepository`를 추가한다.
- [ ] 사용자별 최근 메시지 조회 메서드를 추가한다.
- [ ] soft delete 조건을 일관되게 적용한다.
- [ ] 기존 인메모리 `history`를 제거한다.
- [ ] `ChatMessageResponse` 변환 로직을 추가한다.
- [ ] `GET /api/v1/chat/history`가 DB 기반으로 응답하도록 전환한다.

### Phase 5 확인 메모

- JPA는 `chat_messages` 같은 일반 도메인 테이블에 사용한다.
- pgvector 전용 컬럼은 JPA entity에 억지로 매핑하지 않는다.

## Phase 6. Embedding 파이프라인 구현

- [ ] `EmbeddingProvider` 인터페이스를 추가한다.
- [ ] `OllamaEmbeddingProvider`를 구현한다.
- [ ] notification embedding input 구성 규칙을 정의한다.
- [ ] `title + body + metadata 일부`를 임베딩 입력으로 사용한다.
- [ ] `content_hash`를 생성한다.
- [ ] 동일 `notification_id + content_hash`가 있으면 임베딩 생성을 skip한다.
- [ ] `NotificationEmbeddingRepository`를 native SQL 또는 `JdbcTemplate` 기반으로 구현한다.
- [ ] 알림 저장 후 임베딩 생성 연동 지점을 추가한다.
- [ ] 임베딩 실패 시 알림 저장을 롤백하지 않는다.
- [ ] 임베딩 실패 로그에는 민감 데이터를 남기지 않는다.

### Phase 6 확인 메모

- Phase 0에서는 별도 queue 없이 동기 처리 후 실패 로그를 남긴다.
- Phase 1에서는 Celery 또는 별도 async worker로 이전할 수 있게 책임을 분리한다.

## Phase 7. RAG 검색 구현

- [ ] `RagRetriever` 인터페이스를 추가한다.
- [ ] `RagDocument` DTO를 추가한다.
- [ ] `PgvectorRagRetriever`를 구현한다.
- [ ] 사용자 질문을 embedding으로 변환한다.
- [ ] `embedding <=> query_vector` 기반 cosine distance 검색을 구현한다.
- [ ] 기본 top-k는 `5`로 둔다.
- [ ] user scope isolation을 SQL 조건으로 강제한다.
- [ ] soft delete된 embedding은 검색에서 제외한다.
- [ ] 검색 결과가 없을 때 fallback 흐름을 구현한다.
- [ ] pgvector 쿼리는 QueryDSL 대신 native SQL 또는 `JdbcTemplate`으로 분리한다.

### Phase 7 확인 메모

- QueryDSL은 일반 필터/정렬/조인에 사용할 수 있다.
- pgvector 전용 연산자는 native SQL로 유지하는 것이 Phase 0에서 가장 단순하고 안정적이다.

## Phase 8. PromptBuilder 구현

- [ ] `PromptBuilder`를 추가한다.
- [ ] chat prompt 생성 메서드를 구현한다.
- [ ] daily summary prompt 생성 메서드를 구현한다.
- [ ] system prompt를 중앙화한다.
- [ ] RAG context를 source, title, body summary, priority, created_at, similarity score 형식으로 포함한다.
- [ ] 최근 대화 히스토리를 prompt에 포함한다.
- [ ] 기본 응답 언어를 한국어로 고정한다.
- [ ] context에 없는 사실을 단정하지 않도록 system instruction을 포함한다.
- [ ] 응답 길이는 API `content` 저장 한도를 넘지 않도록 제약한다.

### Phase 8 확인 메모

- prompt는 서비스 메서드 내부에 반복 hardcode하지 않는다.
- 향후 Todo/Analytics LLM 기능도 같은 prompt 경계를 재사용한다.

## Phase 9. ChatService RAG + LLM 전환

- [ ] `generateDummyAiResponse`를 제거한다.
- [ ] 단건 chat 흐름을 실제 RAG + LLM 호출로 교체한다.
- [ ] 사용자 메시지를 먼저 저장한다.
- [ ] 질문 embedding을 생성한다.
- [ ] pgvector top-k 검색을 수행한다.
- [ ] prompt를 생성한다.
- [ ] `LlmProvider.chat`을 호출한다.
- [ ] assistant 메시지를 저장한다.
- [ ] 기존 `ChatMessageResponse` 형식으로 반환한다.
- [ ] LLM 장애 시 `LLM_UNAVAILABLE`로 매핑한다.
- [ ] RAG 검색 결과가 없으면 명확한 fallback 응답을 제공한다.

### Phase 9 확인 메모

- API path, request body, response wrapper는 변경하지 않는다.
- 사용자 인증 연동 전환 시 `DEFAULT_PHASE0_USER_ID` 제거 또는 축소 범위를 별도 검토한다.

## Phase 10. SSE Streaming 전환

- [ ] `GET /api/v1/chat/stream` endpoint를 유지한다.
- [ ] query parameter `content` 계약을 유지한다.
- [ ] 실제 LLM streaming chunk를 SSE로 전달한다.
- [ ] chunk 이벤트 payload는 프론트가 처리 가능한 형식으로 유지한다.
- [ ] 전체 assistant content를 서버에서 누적한다.
- [ ] stream 완료 후 assistant 메시지를 DB에 저장한다.
- [ ] 완료 이벤트로 `done=true`와 `message_id`를 전달한다.
- [ ] streaming timeout을 설정한다.
- [ ] client disconnect 상황을 안전하게 처리한다.

### Phase 10 확인 메모

- 프론트 `ChatRemoteDataSource.streamMessage`는 `data: ` prefix를 제거한 문자열을 전달하므로, payload 구조 변경 시 프론트 파서 영향이 있다.
- Phase 0에서는 기존 chunk/done 흐름을 유지한다.

## Phase 11. DailySummaryService LLM 전환

- [ ] Redis cache key를 user/date 기준으로 분리한다.
- [ ] cache hit이면 LLM을 호출하지 않는다.
- [ ] 오늘 알림 목록을 조회한다.
- [ ] daily summary prompt를 생성한다.
- [ ] `LlmProvider.chat`을 호출한다.
- [ ] `summary`, `date`, `total_messages`, `topics`를 기존 응답 구조로 반환한다.
- [ ] Redis 24시간 캐시를 적용한다.
- [ ] LLM 장애 시 캐시가 있으면 캐시 응답을 반환한다.
- [ ] LLM 장애 시 캐시가 없으면 `LLM_UNAVAILABLE`로 응답한다.

### Phase 11 확인 메모

- 기존 `DailySummaryResponse` 구조를 유지해야 프론트 변경 없이 전환할 수 있다.

## Phase 12. 테스트 및 검증

- [ ] `PromptBuilderTest`를 추가한다.
- [ ] `ChatServiceTest`를 RAG + LLM 흐름 기준으로 갱신한다.
- [ ] `DailySummaryServiceTest`를 LLM/cache/fallback 기준으로 갱신한다.
- [ ] `NotificationEmbeddingServiceTest`를 추가한다.
- [ ] `RagRetrieverTest`를 추가한다.
- [ ] pgvector extension 활성화 통합 테스트를 추가한다.
- [ ] embedding insert 통합 테스트를 추가한다.
- [ ] cosine similarity search 통합 테스트를 추가한다.
- [ ] user scope isolation 테스트를 추가한다.
- [ ] `ChatControllerTest`에서 기존 API 응답 형식을 검증한다.
- [ ] SSE `chunk`/`done` 이벤트 형식을 검증한다.
- [ ] `./gradlew test`를 실행한다.
- [ ] 필요 시 `./gradlew checkstyleMain spotbugsMain`을 실행한다.
- [ ] 로컬 Ollama 모델 설치 상태를 확인한다.
- [ ] `curl`로 Chat API smoke test를 수행한다.

### Phase 12 확인 메모

- 외부 LLM 호출은 단위 테스트에서 mock provider로 대체한다.
- pgvector 통합 테스트는 Testcontainers PostgreSQL 이미지가 pgvector extension을 지원하는지 먼저 확인한다.

## Phase 13. Phase 1 분리 대비 정리

- [ ] `LlmProvider` 구현을 local Ollama와 remote AI Service client로 교체 가능하게 유지한다.
- [ ] `EmbeddingProvider` 구현을 local Ollama와 remote AI Service client로 교체 가능하게 유지한다.
- [ ] `RagRetriever` 책임이 ChatService 내부에 흡수되지 않도록 유지한다.
- [ ] prompt 구성 책임이 각 service에 흩어지지 않도록 유지한다.
- [ ] Python FastAPI, LangChain, Celery, Kafka는 이번 Phase 0 구현 범위에서 제외한다.
- [ ] Backend 컨테이너화는 이번 Phase 0 필수 범위에서 제외한다.

### Phase 13 확인 메모

- Phase 0의 완료 기준은 Spring Boot 모놀리스 내부에서 실제 RAG + LLM 응답이 동작하는 것이다.
- Phase 1의 완료 기준은 같은 Chat API를 유지하면서 내부 provider를 별도 AI Service로 교체하는 것이다.
