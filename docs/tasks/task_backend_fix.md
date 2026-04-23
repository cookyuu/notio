# Backend Fix 개발 체크리스트

> 기준 문서: `docs/plans/plan_fix_llm.md`  
> 대상: `GET /api/v1/chat/stream`, `POST /api/v1/chat`의 자연어 기간 표현을 RAG 검색 조건에 반영  
> 범위: Spring Boot 모놀리스 내부 구현, 기존 Chat API 계약 유지, 기존 RAG + LLM 경계 유지

---

## Phase 0. 범위 확정 및 기존 계약 보호

- [x] `docs/plans/plan_fix_llm.md`를 기준 문서로 확정한다.
- [x] 기존 `GET /api/v1/chat/stream?content=...` API 계약을 변경하지 않는다.
- [x] 기존 `POST /api/v1/chat` request/response 계약을 변경하지 않는다.
- [x] 기존 SSE event 형식인 `chunk`, `done`을 유지한다.
- [x] 기간 필터는 알림 RAG 검색에만 적용하고 최근 대화 히스토리 조회에는 적용하지 않는다.
- [x] 실제 인증 사용자 ID 적용은 이번 작업에서 제외하고 기존 `DEFAULT_PHASE0_USER_ID = 1L` 정책을 유지한다.
- [x] 기간 파싱 실패 또는 미지원 표현은 에러가 아니라 기존 RAG 검색 fallback으로 처리한다.
- [x] LLM이 직접 DB를 조회하는 구조로 변경하지 않는다.

검증 메모:
- 기준 문서: `docs/plans/plan_fix_llm.md`
- `GET /api/v1/chat/stream?content=...`, `POST /api/v1/chat` 계약은 `backend/src/main/java/com/notio/chat/controller/ChatController.java`와 `backend/src/test/java/com/notio/chat/controller/ChatControllerTest.java` 기준으로 유지한다.
- SSE payload의 `chunk`, `done` JSON 형식은 `backend/src/main/java/com/notio/chat/service/ChatService.java`와 `streamReturnsJsonChunkAndDoneDataEvents()` 테스트 기준으로 유지한다.
- 최근 대화 히스토리 조회는 `ChatService.history()`와 `buildChatPrompt()`의 `chatMessageRepository.findRecentByUserId(...)` 경로를 유지하고, 기간 조건은 이후 단계에서 `ragRetriever.retrieve(...)` 쪽에만 연결한다.
- Phase 0 사용자 정책은 `ChatService.DEFAULT_PHASE0_USER_ID = 1L`, `NotificationService.DEFAULT_PHASE0_USER_ID = 1L`를 그대로 유지한다.
- fallback 정책과 LLM/RAG 경계는 `docs/plans/plan_fix_llm.md`의 완료 기준 및 주의사항을 구현 기준으로 고정한다.

## Phase 1. 기간 필터 값 타입 추가

- [ ] `com.notio.ai.rag` 또는 `com.notio.chat.service` 하위에 `TimeRange` record를 추가한다.
- [ ] `TimeRange`는 `Instant startInclusive`, `Instant endExclusive`를 가진다.
- [ ] `startInclusive`는 필수 값으로 다룬다.
- [ ] 상대 기간에서도 `endExclusive`에는 추출 시점의 `now`를 명시적으로 넣는다.
- [ ] SQL 조건 기준을 `created_at >= startInclusive AND created_at < endExclusive`로 통일한다.
- [ ] 값 타입 위치가 RAG, Chat, Prompt 경계를 불필요하게 순환 참조하지 않는지 확인한다.

## Phase 2. 자연어 기간 추출기 구현

- [ ] `ChatTimeRangeExtractor`를 추가한다.
- [ ] 사용자 질문 문자열에서 지원 기간 표현을 찾아 `Optional<TimeRange>`를 반환한다.
- [ ] 기간 표현이 없으면 `Optional.empty()`를 반환한다.
- [ ] 미지원 날짜 범위 표현은 `Optional.empty()`로 fallback한다.
- [ ] 상대 기간 정규식 `(최근|지난)\s*(\d+)\s*(분|시간|일)`을 지원한다.
- [ ] `최근 N분`, `지난 N분`을 `now - N분 <= created_at < now`로 변환한다.
- [ ] `최근 N시간`, `지난 N시간`을 `now - N시간 <= created_at < now`로 변환한다.
- [ ] `최근 N일`, `지난 N일`을 `now - N일 <= created_at < now`로 변환한다.
- [ ] `오늘`을 서버 기본 timezone 기준 오늘 00:00 이상, 내일 00:00 미만으로 변환한다.
- [ ] `어제`를 서버 기본 timezone 기준 어제 00:00 이상, 오늘 00:00 미만으로 변환한다.
- [ ] 상대 기간과 날짜 키워드가 모두 있으면 상대 기간을 우선한다.
- [ ] `Clock`을 생성자 주입받아 테스트에서 현재 시각을 고정할 수 있게 한다.

## Phase 3. RAG 검색 인터페이스 확장

- [ ] `RagRetriever` 시그니처를 `retrieve(Long userId, String question, Optional<TimeRange> timeRange)`로 변경한다.
- [ ] 기존 `RagRetriever` 호출부를 새 시그니처로 갱신한다.
- [ ] 기간 조건이 없는 기존 호출은 `Optional.empty()`를 전달한다.
- [ ] streaming chat과 non-streaming chat이 동일한 RAG 검색 경로를 사용하는지 확인한다.
- [ ] 변경된 인터페이스로 인해 기존 테스트 mock/stub이 깨지는 지점을 정리한다.

## Phase 4. pgvector SQL 기간 조건 적용

- [ ] `PgvectorRagRetriever`에서 기간 조건이 있을 때만 `notifications.created_at` 조건을 추가한다.
- [ ] 기간 조건이 있으면 SQL에 `AND n.created_at >= ?`를 포함한다.
- [ ] 기간 조건이 있으면 SQL에 `AND n.created_at < ?`를 포함한다.
- [ ] 기간 조건이 없으면 기존 SQL과 동일하게 시간 조건을 추가하지 않는다.
- [ ] 기존 `ne.user_id = ?`, `n.user_id = ?` 조건을 유지한다.
- [ ] 기존 `ne.deleted_at IS NULL`, `n.deleted_at IS NULL` 조건을 유지한다.
- [ ] 기존 `ORDER BY ne.embedding <=> ?::vector` 정렬을 유지한다.
- [ ] 기존 `LIMIT ?` 및 `notio.rag.top-k` 기본값을 유지한다.
- [ ] SQL 문자열 조합 시 사용자 입력을 직접 붙이지 않는다.
- [ ] 기간 필터 적용 후에도 결과 의미가 기간 내 전체 알림이 아니라 기간 내 유사도 top-k임을 유지한다.
- [ ] `JdbcTemplate` 파라미터 순서가 기간 조건 유무에 따라 정확히 맞는지 확인한다.

## Phase 5. ChatService 연결

- [ ] `ChatService`에 `ChatTimeRangeExtractor`를 생성자 주입한다.
- [ ] `buildChatPrompt()`에서 RAG 검색 전에 기간을 추출한다.
- [ ] 사용자 메시지 저장 후 `ChatTimeRangeExtractor.extract(userMessage)`를 호출한다.
- [ ] 추출한 `Optional<TimeRange>`를 `RagRetriever.retrieve(...)`에 전달한다.
- [ ] `streamChat()`이 기간 필터 로직을 동일하게 타는지 확인한다.
- [ ] `chat()`이 기간 필터 로직을 동일하게 타는지 확인한다.
- [ ] 최근 대화 10개 조회에는 기간 필터를 적용하지 않는다.
- [ ] 기간 표현이 없는 기존 질문은 기존 RAG 검색과 동일하게 동작하게 한다.

## Phase 6. PromptBuilder 기간 정보 반영

- [ ] `PromptBuilder`의 chat prompt 생성 입력에 적용된 기간 조건을 전달할 수 있게 한다.
- [ ] 기간 조건이 있으면 prompt에 `Applied time filter` 섹션을 포함한다.
- [ ] 기간 조건이 있으면 `startInclusive <= notification.created_at < endExclusive` 범위를 명시한다.
- [ ] 기간 조건이 없으면 prompt에 기간 필터 없음 상태를 명시한다.
- [ ] RAG context의 기존 `created_at` 출력 형식은 유지한다.
- [ ] RAG context가 비어 있으면 해당 기간 조건에 맞는 관련 알림을 찾지 못했다는 취지로 답하도록 유도한다.
- [ ] 기본 응답 언어와 기존 system instruction을 유지한다.

## Phase 7. 단위 테스트 보강

- [ ] `ChatTimeRangeExtractorTest`를 추가한다.
- [ ] `최근 5시간 내의 알림 내역을 요약해줘`가 `now - 5h <= created_at < now`로 변환되는지 검증한다.
- [ ] `지난 30분 동안 중요한 알림 알려줘`가 `now - 30m <= created_at < now`로 변환되는지 검증한다.
- [ ] `최근 3일 알림 요약해줘`가 `now - 3d <= created_at < now`로 변환되는지 검증한다.
- [ ] `오늘 받은 알림 요약해줘`가 오늘 00:00 이상, 내일 00:00 미만으로 변환되는지 검증한다.
- [ ] `어제 알림 정리해줘`가 어제 00:00 이상, 오늘 00:00 미만으로 변환되는지 검증한다.
- [ ] 기간 표현이 없는 질문은 `Optional.empty()`를 반환하는지 검증한다.
- [ ] 미지원 날짜 범위 표현은 `Optional.empty()`를 반환하는지 검증한다.
- [ ] 상대 기간과 날짜 키워드가 모두 있으면 상대 기간을 우선하는지 검증한다.

## Phase 8. RAG 및 ChatService 테스트 보강

- [ ] `PgvectorRagRetrieverTest`에서 기간 조건이 있으면 SQL에 `n.created_at >= ?`가 포함되는지 검증한다.
- [ ] `PgvectorRagRetrieverTest`에서 기간 조건이 있으면 SQL에 `n.created_at < ?`가 포함되는지 검증한다.
- [ ] `PgvectorRagRetrieverTest`에서 기간 조건이 있으면 start/end 파라미터가 `JdbcTemplate`에 전달되는지 검증한다.
- [ ] `PgvectorRagRetrieverTest`에서 기간 조건이 없으면 기존 SQL 조건과 파라미터 순서를 유지하는지 검증한다.
- [ ] 기존 embedding dimension 검증 테스트를 유지한다.
- [ ] `ChatServiceTest`에서 streaming chat 기간 표현 질문이 `RagRetriever.retrieve(..., timeRange)`를 호출하는지 검증한다.
- [ ] `ChatServiceTest`에서 non-streaming chat도 동일하게 기간 조건을 전달하는지 검증한다.
- [ ] `ChatServiceTest`에서 기간 표현이 없으면 `Optional.empty()`로 RAG 검색하는지 검증한다.
- [ ] 기존 LLM streaming chunk 누적과 assistant message 저장 동작 테스트를 유지한다.

## Phase 9. PromptBuilder 테스트 보강

- [ ] `PromptBuilderTest`에서 기간 조건이 있을 때 prompt에 `Applied time filter`가 포함되는지 검증한다.
- [ ] `PromptBuilderTest`에서 기간 조건이 없을 때 prompt에 기간 필터 없음이 포함되는지 검증한다.
- [ ] `PromptBuilderTest`에서 RAG context의 `created_at` 출력 형식이 유지되는지 검증한다.
- [ ] RAG context가 비어 있을 때 fallback 답변 지침이 prompt에 포함되는지 검증한다.

## Phase 10. 최종 검증

- [ ] `./gradlew test`를 실행한다.
- [ ] `최근 5시간 내의 알림 내역을 요약해줘` 요청 시 RAG SQL에 `created_at` 기간 조건이 적용되는지 확인한다.
- [ ] 기존 기간 표현 없는 질문이 기존 RAG 검색과 동일하게 동작하는지 확인한다.
- [ ] 기존 SSE event 형식 `chunk`, `done`이 유지되는지 확인한다.
- [ ] 프론트엔드 API 계약 변경 없이 기존 Chat 화면에서 동작하는지 확인한다.
- [ ] 지원하지 않는 기간 표현이 에러 없이 기존 RAG 검색으로 fallback하는지 확인한다.
