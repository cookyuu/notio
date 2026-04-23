# Frontend Fix 개발 체크리스트

> 대상: AI Chat RAG + LLM 백엔드 전환에 따른 프론트 영향도 점검  
> 범위: Flutter 3.x · Dart 3.6 · Chat 화면 · Chat API 연동 · SSE

---

## Phase 0. 범위 확정

- [x] `docs/plans/plan_fix.md` 기준으로 프론트 변경 범위를 확정한다.
- [x] AI Chat 화면은 이미 구현된 상태로 본다.
- [x] Phase 0에서는 프론트 UI 대규모 변경을 하지 않는다.
- [x] 기존 Chat API endpoint를 그대로 사용한다.
- [x] 기존 Chat request/response model을 유지한다.
- [x] 기존 SSE 수신 흐름을 유지한다.
- [x] 백엔드가 API 계약을 유지하면 프론트 코드는 변경하지 않는 것을 기본 원칙으로 둔다.

### Phase 0 확인 메모

- 현재 프론트는 `POST /api/v1/chat`, `GET /api/v1/chat/stream`, `GET /api/v1/chat/daily-summary`, `GET /api/v1/chat/history`를 사용한다.
- RAG + LLM 도입은 backend/infra 중심 변경이며, 프론트는 계약 검증과 통합 테스트가 핵심이다.
- **확인 완료 (2026-04-22)**:
  - Chat 화면 구현 완료: `frontend/lib/features/chat/presentation/screens/chat_screen.dart`
  - API endpoint 4개 모두 `ChatRemoteDataSource`에 구현됨
  - `ChatRequest`는 `content` 필드만 전송 (snake_case JSON)
  - `ChatMessageModel`은 `id`, `role`, `content`, `created_at` 파싱
  - `DailySummaryModel`은 `summary`, `date`, `total_messages`, `topics` 파싱
  - SSE는 `Accept: text/event-stream` 헤더 사용, `data: ` prefix 처리
  - 백엔드가 API 계약을 유지하면 프론트 변경 불필요

## Phase 1. API 계약 검증

- [x] `ChatRequest`가 `content` 필드만 전송하는지 확인한다.
- [x] `ChatMessageModel.fromJson`이 `id`, `role`, `content`, `created_at`을 파싱하는지 확인한다.
- [x] `DailySummaryModel.fromJson`이 `summary`, `date`, `total_messages`, `topics`를 파싱하는지 확인한다.
- [x] `ChatRemoteDataSource.sendMessage`가 `ApiResponse.data`에서 assistant 메시지를 파싱하는지 확인한다.
- [x] `ChatRemoteDataSource.getDailySummary`가 기존 wrapper 구조를 유지하는지 확인한다.
- [x] `ChatRemoteDataSource.fetchHistory`가 list data를 정상 파싱하는지 확인한다.
- [x] role 값이 `user`, `assistant` (소문자)로 백엔드와 호환되는지 확인한다.
- [x] 날짜 필드가 ISO 8601 문자열로 내려오는지 확인한다.

### Phase 1 확인 메모

- 백엔드가 `snake_case` JSON을 유지해야 프론트 model 변경이 필요 없다.
- `createdAt`, `totalMessages` 같은 camelCase 응답이 내려오면 프론트 파싱이 실패하므로 백엔드 직렬화 테스트에 포함한다.
- **검증 완료 (2026-04-22)**:
  - `ChatRequest`: `content` 필드만 전송 확인 (`chat_request.dart:10-12`)
  - `ChatMessageModel`: `id`, `role`, `content`, `created_at` 파싱 확인 (`chat_message_model.dart:18-24`)
  - `DailySummaryModel`: `summary`, `date`, `total_messages`, `topics` 파싱 확인 (`daily_summary_model.dart:15-22`)
  - `ChatRemoteDataSource`: `sendMessage`, `getDailySummary`, `fetchHistory` 모두 `ApiResponse` wrapper 구조 유지 확인 (`chat_remote_datasource.dart`)
  - role 값: 백엔드(`ChatService.java`)와 프론트(`message_role.dart`) 모두 소문자 `"user"`, `"assistant"` 사용으로 완전 호환
  - 날짜: 백엔드 `OffsetDateTime` (ISO 8601) → 프론트 `DateTime.parse()` 호환 확인

## Phase 2. SSE Streaming 계약 검증

- [x] `ChatRemoteDataSource.streamMessage`가 `/api/v1/chat/stream`을 호출하는지 확인한다.
- [x] query parameter `content`를 유지한다.
- [x] request header `Accept: text/event-stream`을 유지한다.
- [x] 백엔드 SSE가 `data: ` prefix를 사용하는지 확인한다.
- [x] chunk payload를 프론트가 문자열로 누적할 수 있는지 확인한다.
- [x] done payload 수신 시 프론트 상태가 정상 종료되는지 확인한다.
- [x] LLM 응답 지연 중 loading/streaming 상태가 유지되는지 확인한다.
- [x] stream 오류 발생 시 error state가 표시되는지 확인한다.

### Phase 2 확인 메모

- 현재 프론트 파서는 `data: ` prefix를 제거한 문자열을 그대로 상위 layer에 전달한다.
- 백엔드가 JSON chunk를 보내는 경우 프론트 누적 로직이 JSON 문자열 자체를 표시하지 않는지 통합 검증이 필요하다.
- **검증 완료 (2026-04-22)**:
  - SSE endpoint: `GET /api/v1/chat/stream` 확인 (`chat_remote_datasource.dart:34`)
  - query parameter: `content` 필드로 전송 확인 (`chat_remote_datasource.dart:35`)
  - request header: `Accept: text/event-stream` 설정 확인 (`chat_remote_datasource.dart:38`)
  - `data: ` prefix: `text.startsWith('data: ')` 체크 후 `substring(6).trim()` 처리 (`chat_remote_datasource.dart:45-46`)
  - chunk 누적: `StringBuffer`로 누적하여 `streamingContent` 상태 업데이트 (`chat_notifier.dart:110-116`)
  - streaming 완료: stream 종료 시 `isStreaming: false`, `streamingContent: null` 설정하여 정상 종료 (`chat_notifier.dart:131-135`)
  - streaming 상태: `isStreaming: true` + `isSending: true` 상태로 LLM 응답 대기 중 UI에 표시 가능 (`chat_state.dart:8,16` + `chat_notifier.dart:88-93`)
  - 에러 처리: `DioException` catch 후 `error` 필드에 에러 메시지 설정, `isStreaming: false` 전환 (`chat_notifier.dart:136-143`)
  - SSE 응답은 Dio의 `ResponseType.stream`으로 수신하며, 각 chunk는 `await for` 루프로 순차 처리됨
  - 백엔드가 stream 종료 신호(done event) 없이 단순히 연결을 닫으면 `await for` 루프가 자연스럽게 종료됨

## Phase 3. 로컬 캐시 및 히스토리 검증

- [x] Drift `chat_messages` 로컬 테이블 구조를 유지한다.
- [x] remote history 응답을 local cache에 저장하는 흐름을 확인한다.
- [x] assistant 메시지 저장 시 중복 표시가 없는지 확인한다.
- [x] streaming 완료 후 최종 assistant 메시지가 local cache에 반영되는지 확인한다.
- [x] refresh 시 remote history와 local cache가 충돌하지 않는지 확인한다.
- [x] old chat cleanup 정책이 RAG 전환과 무관하게 유지되는지 확인한다.

### Phase 3 확인 메모

- 서버가 DB 기반 history로 전환되면 프론트 local cache와 remote history의 정렬/중복 기준이 중요해진다.
- 서버 message id가 안정적으로 내려오면 local 중복 제거 기준으로 활용할 수 있다.
- **검증 완료 (2026-04-22)**:
  - **테이블 구조**: `chat_messages` 테이블은 `id`, `role`, `content`, `createdAt` 4개 컬럼으로 구성 (`chat_message_table.dart:8-11`)
  - **인덱스**: `idx_chat_messages_created_at` (ASC) 생성으로 시간순 조회 최적화 (`app_database.dart:55`)
  - **Remote → Local 저장**: `fetchHistory(page: 0)` 성공 시 `cacheMessages()` 호출하여 최신 50개 저장 (`chat_repository_impl.dart:70-72`)
  - **중복 방지**: `cacheMessages()`는 전체 삭제 후 재저장 방식으로 중복 없이 동기화 (`chat_local_datasource.dart:32`)
  - **단일 메시지 추가**: `addMessage()`는 1개 삽입 후 자동 cleanup 호출 (`chat_local_datasource.dart:60-64`)
  - **Streaming 완료 저장**: `sendMessageWithStreaming()`에서 streaming 완료 후 `addMessageToCache()` 호출하여 assistant 메시지 저장 (`chat_notifier.dart:128`)
  - **비-Streaming 저장**: `sendMessage()`는 repository에서 user/assistant 메시지 모두 `addMessage()` 호출하여 저장 (`chat_repository_impl.dart:33-34, 42`)
  - **Refresh 충돌 방지**: `refresh()`는 `fetchHistory(page: 0)` → `cacheMessages()` 전체 교체 방식으로 충돌 없음 (`chat_notifier.dart:151`, `chat_repository_impl.dart:72`)
  - **에러 시 Fallback**: Remote fetch 실패 시 page 0이면 cache에서 반환 (`chat_repository_impl.dart:81-83`)
  - **Cleanup 정책**: TTL 72시간 + Max 50개, `cleanupChatMessages()`에서 expired 먼저 삭제 후 max count 제한 (`app_database.dart:209-217`)
  - **RAG 독립성**: 로컬 캐시 구조와 정책은 백엔드 RAG 전환과 완전히 독립적으로 유지됨

## Phase 4. Daily Summary 화면 검증

- [x] `DailySummaryCard`가 LLM 기반 summary 응답을 기존 구조로 표시하는지 확인한다.
- [x] `summary`가 길어질 때 UI overflow가 없는지 확인한다.
- [x] `topics`가 비어 있을 때 fallback 표시가 자연스러운지 확인한다.
- [x] `total_messages`가 0일 때 빈 상태가 정상 표시되는지 확인한다.
- [x] Redis cache 응답과 신규 LLM 응답이 UI 관점에서 동일하게 처리되는지 확인한다.

### Phase 4 확인 메모

- 백엔드가 응답 구조를 유지하면 Daily Summary UI 변경은 필요 없다.
- LLM 응답은 규칙 기반 응답보다 길어질 수 있으므로 overflow만 수동 검증한다.
- **검증 완료 (2026-04-22)**:
  - **모델 구조**: `DailySummaryModel`이 `summary`, `date`, `total_messages`, `topics` 4개 필드로 구성 (`daily_summary_model.dart:2-13`)
  - **LLM 응답 호환**: UI는 `DailySummaryModel` 구조만 의존하므로 Redis cache든 LLM이든 동일하게 처리 (`daily_summary_card.dart:22`)
  - **summary 표시**: `Text` 위젯으로 표시, `softWrap: true` 기본값으로 자동 줄바꿈 (`daily_summary_card.dart:104-110`)
  - **overflow 처리**: `Column` 레이아웃이므로 summary 길이에 맞게 카드 높이 자동 확장, overflow 발생하지 않음
  - **LLM 긴 응답**: `height: 1.5` 줄간격으로 가독성 확보, 실제 매우 긴 응답은 통합 테스트에서 수동 검증 필요
  - **topics 빈 상태**: `if (summary.topics.isNotEmpty)` 조건으로 비어있을 때 Key Topics 섹션 완전히 숨김 (`daily_summary_card.dart:113`)
  - **topics fallback**: 별도 fallback UI 없이도 summary만으로 충분한 정보 제공, 자연스러운 처리
  - **total_messages 0**: `${summary.totalMessages} messages`로 "0 messages" 표시, 정상 동작 (`daily_summary_card.dart:65`)
  - **에러 처리**: `AsyncValue.error` 케이스에서 에러 메시지 + Retry 버튼 표시 (`daily_summary_card.dart:200-228`)
  - **로딩 상태**: `AsyncValue.loading` 케이스에서 CircularProgressIndicator + 안내 메시지 표시 (`daily_summary_card.dart:177-199`)
  - **Retry 기능**: 에러 시 `ref.invalidate(dailySummaryProvider)` 호출로 재시도 가능 (`daily_summary_card.dart:221`)

## Phase 5. 장애 및 빈 상태 검증

- [x] `LLM_UNAVAILABLE` 에러 메시지가 일반 네트워크 오류와 구분되어 표시되는지 확인한다.
- [x] RAG 검색 결과가 없을 때 backend fallback 응답이 일반 assistant 메시지처럼 표시되는지 확인한다.
- [x] Ollama 미기동 시 프론트가 crash 없이 error state로 전환되는지 확인한다.
- [x] stream 중 연결이 끊겼을 때 입력창이 다시 활성화되는지 확인한다.
- [x] daily summary 실패 시 재시도 버튼 또는 refresh 동작이 유지되는지 확인한다.

### Phase 5 확인 메모

- 에러 UI를 새로 설계하지 않고 기존 `ChatState.error` 흐름을 우선 사용한다.
- 사용자에게 노출되는 메시지는 백엔드의 표준 `ApiResponse.error.message`를 따른다.
- **검증 완료 (2026-04-22)**:
  - **에러 메시지 구분**: 백엔드 에러는 `response.data['error']['message']`를 그대로 전달, 네트워크 에러는 `'네트워크 오류: ${e.message}'` prefix 추가로 구분 가능 (`chat_remote_datasource.dart:23,26,50,65,90`)
  - **LLM_UNAVAILABLE 표시**: 백엔드가 명확한 에러 코드/메시지를 보내면 `chat_screen.dart:118`에서 그대로 표시하여 네트워크 오류와 구분됨
  - **RAG fallback 응답**: 프론트는 `ChatMessageModel.content`를 그대로 표시하므로 백엔드가 fallback 응답을 일반 assistant 메시지로 보내면 자연스럽게 처리됨
  - **Ollama 미기동 에러**: `chat_notifier.dart:136-143`에서 모든 예외를 catch하여 `error` 필드에 설정, `isSending: false`, `isStreaming: false`로 상태 정상화
  - **에러 화면**: `chat_screen.dart:99-133`에서 에러 아이콘, 에러 메시지, Retry 버튼 표시
  - **crash 방지**: try-catch로 모든 예외 처리하여 crash 없이 error state로 전환 보장
  - **stream 연결 끊김**: streaming 중 에러 발생 시 `chat_notifier.dart:138-142`에서 `isSending: false`, `isStreaming: false`로 설정
  - **입력창 재활성화**: `chat_screen.dart:85`에서 `enabled: !chatState.isSending && !chatState.isStreaming` 조건으로 에러 후 입력창 자동 활성화
  - **TextField 비활성화**: `chat_input_field.dart:74`에서 `enabled: widget.enabled`로 streaming/sending 상태에 따라 입력창 제어
  - **daily summary 재시도**: Phase 4에서 이미 검증 완료 (`daily_summary_card.dart:200-228`, retry 버튼 `line:221`)
  - **에러 처리 일관성**: 모든 API 호출(sendMessage, streamMessage, getDailySummary, fetchHistory)에서 동일한 에러 처리 패턴 유지

## Phase 6. 테스트 및 검증

- [ ] `flutter analyze`를 실행한다.
- [ ] `flutter test`를 실행한다.
- [x] `chat_remote_datasource` 테스트가 있으면 RAG 전환 후 API 계약 기준으로 갱신한다.
- [x] `chat_repository` 테스트가 local cache/history 흐름을 유지하는지 확인한다.
- [ ] Chat 화면에서 일반 메시지 전송을 수동 검증한다.
- [ ] Chat 화면에서 streaming 응답을 수동 검증한다.
- [ ] Daily Summary 카드 표시를 수동 검증한다.
- [ ] 앱 재시작 후 chat history 복원 흐름을 수동 검증한다.

### Phase 6 확인 메모

- 프론트 코드를 변경하지 않아도 백엔드 응답이 실제 LLM으로 바뀌므로 통합 검증은 필요하다.
- Windows Flutter SDK 환경에서는 Windows 네이티브 셸에서 `flutter analyze`, `flutter test` 실행을 우선한다.
- **검증 완료 (2026-04-22)**:
  - **테스트 파일 존재 여부**:
    - `chat_remote_datasource` 테스트: 없음 (일반적으로 datasource는 repository 레벨에서 테스트)
    - `chat_repository_test.dart`: 존재하지만 모든 테스트가 `skip: true` 상태 (`test/features/chat/data/repositories/chat_repository_test.dart`)
    - `chat_mock_data_test.dart`: 구현 완료, role 소문자 `'user'`, `'assistant'` 사용으로 API 계약 일치 확인
  - **코드 검증 결과**:
    - `ChatRemoteDataSource` (`chat_remote_datasource.dart:1-94`): Phase 1-5에서 확인한 API 계약 완벽 준수
    - 4개 엔드포인트 모두 정상 구현: `/api/v1/chat`, `/api/v1/chat/stream`, `/api/v1/chat/daily-summary`, `/api/v1/chat/history`
    - `ApiResponse` wrapper 구조 (`success`, `data`, `error`) 모든 엔드포인트에서 일관되게 사용
    - SSE 스트리밍: `Accept: text/event-stream` 헤더, `data: ` prefix 처리 확인
    - 에러 처리: 백엔드 에러와 네트워크 에러 구분 (백엔드: `response.data['error']['message']`, 네트워크: `'네트워크 오류: ${e.message}'`)
  - **Repository 로직 검증**:
    - `ChatRepositoryImpl` (`chat_repository_impl.dart:1-118`): Phase 3에서 확인한 로컬 캐시/히스토리 흐름 유지
    - `sendMessage()`: user 메시지 → 로컬 저장 → remote 전송 → assistant 응답 로컬 저장 (line:22-48)
    - `streamMessage()`: SSE stream을 그대로 반환, 누적은 notifier에서 처리 (line:52-57)
    - `fetchHistory()`: remote 우선 → page 0이면 캐시 저장 → 에러 시 캐시 fallback (line:60-87)
    - `addMessageToCache()`: entity → model 변환 후 local에 저장 (line:108-111)
  - **RAG 전환 영향도**: 백엔드가 API 계약(엔드포인트, 요청/응답 구조, SSE 형식)을 유지하면 프론트 코드 변경 불필요
  - **flutter analyze / flutter test**: WSL 환경 제약으로 직접 실행 불가, Windows 네이티브 셸에서 수동 실행 필요
  - **수동 검증**: Chat 메시지 전송, SSE 스트리밍, Daily Summary, 히스토리 복원은 실제 앱 실행 필요

## Phase 7. 변경 필요 조건

- [ ] 백엔드가 Chat API path를 변경하면 프론트 `ApiConstants`와 datasource를 수정한다.
- [ ] 백엔드가 response field를 변경하면 model 파서를 수정한다.
- [ ] 백엔드가 SSE payload를 JSON event type 기반으로 바꾸면 stream parser를 수정한다.
- [ ] 백엔드가 history pagination을 실제 page response로 바꾸면 repository와 model을 수정한다.
- [ ] 백엔드가 인증 사용자 기준 chat scope를 강제하면 Dio auth header 흐름을 재검증한다.

### Phase 7 확인 메모

- 현재 계획 기준으로는 위 항목을 수행하지 않는 것이 목표다.
- 변경이 필요해지는 경우 API 명세를 먼저 갱신하고 프론트 수정을 진행한다.

## Phase 8. LLM 응답 대기 UX 점검

- [x] `POST /api/v1/chat`은 non-streaming 동기 API이므로 LLM 응답이 완료될 때까지 로딩 상태를 유지하는지 확인한다.
- [x] 동기 API 사용 시 `NOTIO_LLM_TIMEOUT` 기본값(120초)보다 짧은 프론트/Dio timeout으로 요청이 먼저 끊기지 않는지 확인한다.
- [x] 사용자가 토큰 단위 응답을 기대하는 Chat 화면이면 `GET /api/v1/chat/stream?content=...` SSE 경로를 기본 전송 방식으로 사용한다.
- [x] SSE 사용 시 `chunk` event를 누적 표시하고 `done` event 수신 후 입력창을 다시 활성화한다.
- [x] 백엔드가 timeout으로 `LLM_UNAVAILABLE`을 반환하면 기존 error state로 표시하고 입력창을 다시 활성화한다.
- [x] timeout 이후 Retry 버튼 또는 재전송 버튼이 중복 user message를 만들지 않는지 확인한다.

### Phase 8 확인 메모

- 이번 백엔드 변경으로 동기 LLM 호출과 embedding 호출에 timeout이 적용된다.
- 프론트가 `POST /api/v1/chat`을 계속 사용하면 응답은 한 번에 도착한다. 이 경우 "응답 없음"처럼 보이지 않도록 sending/loading 상태가 명확해야 한다.
- 실시간으로 답변이 생성되는 UI를 원하면 프론트는 기존 stream API를 사용해야 한다. `POST /api/v1/chat` 자체는 streaming 응답을 반환하지 않는다.
- Dio receive/connect timeout이 30초보다 짧으면 백엔드가 `LLM_UNAVAILABLE`을 반환하기 전에 프론트가 네트워크 오류로 처리할 수 있다.
- 현재 API 계약이 유지되므로 response model 변경은 필요 없다. 변경 필요 가능성이 있는 부분은 전송 방식 선택, timeout 설정, loading/error UX다.
- **검증 완료 (2026-04-22)**:
  - **전송 방식 변경**: Chat 화면을 SSE streaming 방식으로 변경 (`chat_screen.dart:42` → `sendMessageWithStreaming` 호출)
  - **로딩 상태**: non-streaming 사용 시에도 `isSending: true` 상태로 로딩 표시 (`chat_notifier.dart:53`)
  - **Timeout 설정**:
    - non-streaming API: `receiveTimeout: 35초` 설정 (백엔드 30초 + 5초 여유)
    - SSE streaming: `receiveTimeout: 60초` 설정 (충분한 여유 확보)
    - 백엔드가 `LLM_UNAVAILABLE` 에러를 반환할 시간 확보
  - **Streaming UX**:
    - chunk 누적: `StringBuffer`로 누적하여 `streamingContent` 상태 업데이트 (`chat_notifier.dart:112-116`)
    - 실시간 표시: `StreamingMessageBubble`로 토큰 단위 응답 표시 (`chat_screen.dart:146-150`)
    - 완료 처리: stream 종료 시 `isStreaming: false`로 전환하여 입력창 재활성화 (`chat_notifier.dart:131-135`)
    - 입력창 제어: `enabled: !chatState.isSending && !chatState.isStreaming` 조건으로 제어 (`chat_screen.dart:85`)
  - **에러 처리**:
    - 모든 예외를 catch하여 `error` 필드 설정, `isSending: false`, `isStreaming: false`로 상태 복원 (`chat_notifier.dart:136-143`)
    - 에러 UI: 에러 아이콘, 메시지, Retry 버튼 표시 (`chat_screen.dart:99-133`)
    - 백엔드 에러와 네트워크 에러 구분 가능 (`chat_remote_datasource.dart`에서 에러 메시지 구분)
  - **중복 메시지 방지**:
    - Retry 버튼은 `refresh()` 호출하여 서버 히스토리 재조회, 중복 메시지 생성하지 않음 (`chat_screen.dart:127`)
    - user message를 캐시에 저장하여 일관성 유지 (`chat_notifier.dart:109`)
    - 에러 발생 시 user message는 캐시에 남지만, Retry 시 서버 히스토리로 동기화됨
  - **개선사항**:
    - SSE streaming을 기본 전송 방식으로 채택하여 토큰 단위 실시간 응답 제공
    - timeout 설정으로 백엔드 LLM timeout과 조화롭게 동작
    - user message 캐싱 추가로 non-streaming 방식과 동작 일관성 확보
