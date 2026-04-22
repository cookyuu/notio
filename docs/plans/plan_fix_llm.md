# LLM Chat 자연어 기간 필터 개선 계획

> 대상: `GET /api/v1/chat/stream` 및 `POST /api/v1/chat`에서 자연어 기간 표현을 RAG 검색 조건으로 반영  
> 기준: 프론트엔드 API 계약 유지, Spring Boot 모놀리스 내부 구현, 기존 RAG + LLM 경계 유지

---

## 1. 목표

현재 Chat API는 사용자가 `최근 5시간 내의 알림 내역을 요약해줘`처럼 기간을 포함해 질문해도 DB 검색 단계에서는 시간 조건을 적용하지 않는다.

현재 동작은 다음과 같다.

- 사용자 질문을 embedding으로 변환한다.
- `notification_embeddings`와 `notifications`를 pgvector 유사도 기준으로 검색한다.
- 검색 결과 top-k를 LLM prompt의 RAG context로 넣는다.
- `created_at`은 context에 포함되지만 SQL 조건으로 사용되지는 않는다.

이번 개선의 목표는 다음과 같다.

- 자연어 질문에서 기간 표현을 추출한다.
- 추출한 기간을 `notifications.created_at` 검색 조건에 반영한다.
- 기존 `/api/v1/chat/stream?content=...` API 계약은 변경하지 않는다.
- streaming chat과 non-streaming chat이 동일한 기간 필터 로직을 사용하게 한다.

---

## 2. 지원 범위

### 2.1 v1 지원 표현

다음 표현을 우선 지원한다.

- `최근 N분`
- `지난 N분`
- `최근 N시간`
- `지난 N시간`
- `최근 N일`
- `지난 N일`
- `오늘`
- `어제`

예시:

```text
최근 5시간 내의 알림 내역을 요약해줘
지난 30분 동안 중요한 알림 알려줘
오늘 받은 알림 요약해줘
어제 GitHub 알림 정리해줘
```

### 2.2 v1 제외 범위

다음 표현은 v1에서 지원하지 않는다.

- `4월 20일부터 4월 22일까지`
- `이번 주`
- `지난달`
- `오전 9시부터 오후 3시까지`
- 복수 기간 조건 조합

지원하지 않는 표현은 기간 필터 없이 기존 RAG 검색으로 fallback한다.

---

## 3. 구현 방향

### 3.1 기간 값 타입 추가

`com.notio.ai.rag` 또는 `com.notio.chat.service` 하위에 기간 필터 값 타입을 추가한다.

```java
public record TimeRange(
        Instant startInclusive,
        Instant endExclusive
) {
}
```

원칙:

- `startInclusive`는 필수다.
- `endExclusive`는 상대 기간에서도 `now`를 넣어 명시적으로 고정한다.
- SQL 조건은 `created_at >= startInclusive AND created_at < endExclusive`로 통일한다.

### 3.2 자연어 기간 추출기 추가

`ChatTimeRangeExtractor`를 추가한다.

책임:

- 사용자 질문 문자열에서 기간 표현을 찾는다.
- 기간이 있으면 `Optional<TimeRange>`를 반환한다.
- 기간이 없거나 지원하지 않는 표현이면 `Optional.empty()`를 반환한다.

파싱 규칙:

- 상대 기간 정규식:
  - `(최근|지난)\s*(\d+)\s*(분|시간|일)`
- 날짜 키워드:
  - `오늘`
  - `어제`
- 상대 기간과 날짜 키워드가 모두 있으면 상대 기간을 우선한다.
- `Clock`을 주입받아 테스트에서 현재 시각을 고정할 수 있게 한다.
- `오늘`, `어제` 계산은 서버 기본 timezone 기준으로 한다.

### 3.3 RAG 검색 인터페이스 확장

`RagRetriever` 인터페이스를 기간 조건을 받을 수 있게 변경한다.

```java
List<RagDocument> retrieve(Long userId, String question, Optional<TimeRange> timeRange);
```

기존 호출부는 모두 새 시그니처로 변경한다.

### 3.4 pgvector SQL에 기간 조건 추가

`PgvectorRagRetriever`는 기간 조건이 있을 때만 `notifications.created_at` 조건을 추가한다.

기간이 있는 경우:

```sql
AND n.created_at >= ?
AND n.created_at < ?
```

기간이 없는 경우:

```sql
-- 기존 SQL과 동일하게 시간 조건 없음
```

기존 조건은 유지한다.

- `ne.user_id = ?`
- `n.user_id = ?`
- `ne.deleted_at IS NULL`
- `n.deleted_at IS NULL`
- `ORDER BY ne.embedding <=> ?::vector`
- `LIMIT ?`

주의:

- 기간 필터 적용 후에도 결과는 기간 내 전체 알림이 아니라 기간 내 유사도 top-k다.
- `top-k` 기본값은 기존 `notio.rag.top-k` 설정을 유지한다.

### 3.5 ChatService에 기간 추출 연결

`ChatService.buildChatPrompt()`에서 RAG 검색 전에 기간을 추출한다.

순서:

```text
사용자 질문 수신
  -> 사용자 메시지 저장
  -> ChatTimeRangeExtractor.extract(userMessage)
  -> RagRetriever.retrieve(userId, userMessage, timeRange)
  -> 최근 대화 10개 조회
  -> PromptBuilder.buildChatPrompt(...)
  -> LLM 호출
```

`streamChat()`과 `chat()` 모두 `buildChatPrompt()`를 거치므로 두 API에 동일하게 적용한다.

### 3.6 PromptBuilder에 기간 조건 명시

LLM이 검색 범위를 알 수 있도록 prompt에 기간 정보를 추가한다.

예시:

```text
Applied time filter:
2026-04-22T05:00:00Z <= notification.created_at < 2026-04-22T10:00:00Z
```

기간이 없으면 다음처럼 명시한다.

```text
Applied time filter:
- 없음
```

RAG context가 비어 있으면 LLM이 다음 취지로 답하도록 유도한다.

```text
해당 기간 조건에 맞는 관련 알림을 찾지 못했습니다.
```

---

## 4. 테스트 계획

### 4.1 ChatTimeRangeExtractorTest

필수 테스트:

- `최근 5시간 내의 알림 내역을 요약해줘`
  - `now - 5h <= created_at < now`
- `지난 30분 동안 중요한 알림 알려줘`
  - `now - 30m <= created_at < now`
- `최근 3일 알림 요약해줘`
  - `now - 3d <= created_at < now`
- `오늘 받은 알림 요약해줘`
  - 오늘 00:00 이상, 내일 00:00 미만
- `어제 알림 정리해줘`
  - 어제 00:00 이상, 오늘 00:00 미만
- 기간 표현이 없는 질문
  - `Optional.empty()`
- 지원하지 않는 날짜 범위 표현
  - `Optional.empty()`

### 4.2 PgvectorRagRetrieverTest

필수 테스트:

- 기간 조건이 있으면 SQL에 `n.created_at >= ?`와 `n.created_at < ?`가 포함된다.
- 기간 조건이 있으면 start/end 파라미터가 `JdbcTemplate`에 전달된다.
- 기간 조건이 없으면 기존 SQL 조건과 파라미터 순서를 유지한다.
- embedding dimension 검증은 기존 테스트를 유지한다.

### 4.3 ChatServiceTest

필수 테스트:

- streaming chat에서 기간 표현이 있는 질문을 받으면 `RagRetriever.retrieve(..., timeRange)`가 호출된다.
- non-streaming chat에서도 동일하게 기간 조건이 전달된다.
- 기간 표현이 없으면 `Optional.empty()`로 RAG 검색한다.
- LLM streaming chunk 누적과 assistant message 저장 동작은 기존과 동일하다.

### 4.4 PromptBuilderTest

필수 테스트:

- 기간 조건이 있을 때 prompt에 `Applied time filter`가 포함된다.
- 기간 조건이 없을 때 prompt에 기간 필터 없음이 포함된다.
- RAG context의 `created_at` 출력은 기존 형식을 유지한다.

---

## 5. 완료 기준

- `최근 5시간 내의 알림 내역을 요약해줘` 요청 시 RAG SQL에 `created_at` 기간 조건이 적용된다.
- 기존 SSE event 형식은 유지된다.
  - `event: chunk`
  - `event: done`
- 프론트엔드 수정 없이 기존 Chat 화면에서 동작한다.
- `./gradlew test`가 통과한다.
- 기간 표현이 없는 기존 질문은 기존 RAG 검색과 동일하게 동작한다.

---

## 6. 주의사항

- LLM이 직접 DB를 조회하는 방식으로 바꾸지 않는다.
- SQL 문자열 조합 시 사용자 입력을 직접 붙이지 않는다.
- 기간 파싱 실패는 에러가 아니라 기존 검색 fallback으로 처리한다.
- 실제 인증 사용자 ID 적용은 별도 작업으로 분리한다. 이번 작업에서는 기존 `DEFAULT_PHASE0_USER_ID = 1L` 정책을 변경하지 않는다.
- 기간 필터는 알림 RAG 검색에만 적용한다. 최근 대화 히스토리 조회에는 적용하지 않는다.
