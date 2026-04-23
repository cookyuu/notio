# Frontend Fix 개발 체크리스트

> 기준 문서: `docs/plans/plan_fix_llm.md`  
> 대상: 자연어 기간 필터 개선에 따른 Flutter Chat 화면 영향도 점검  
> 범위: 기존 Chat API 계약 유지 검증, SSE 수신 흐름 유지 검증, 필요 시 최소 수정

---

## Phase 0. 범위 확정

- [x] `docs/plans/plan_fix_llm.md`를 기준 문서로 확정한다.
- [x] 이번 개선은 백엔드 RAG 검색 조건 개선이 중심임을 확인한다.
- [x] 프론트엔드 API request/response model 변경은 기본 범위에서 제외한다.
- [x] 기존 Chat 화면 UI 대규모 변경은 하지 않는다.
- [x] 기존 `POST /api/v1/chat` 호출 흐름을 유지한다.
- [x] 기존 `GET /api/v1/chat/stream?content=...` 호출 흐름을 유지한다.
- [x] 사용자가 자연어로 기간을 입력하면 별도 프론트 파싱 없이 기존 `content`로 그대로 전송한다.
- [x] 백엔드가 API 계약을 유지하면 프론트 코드는 변경하지 않는 것을 목표로 한다.

## Phase 1. Chat API 계약 재검증

- [x] `ChatRequest`가 `content` 필드만 전송하는지 확인한다.
- [x] 자연어 기간 표현이 포함된 메시지도 별도 escaping/가공 없이 `content`에 담기는지 확인한다.
- [x] `ChatMessageModel.fromJson`이 기존 `id`, `role`, `content`, `created_at` 필드를 파싱하는지 확인한다.
- [x] 백엔드 응답에 기간 필터용 신규 필드가 추가되지 않아도 프론트가 정상 동작하는지 확인한다.
- [x] 백엔드가 신규 필드를 추가하더라도 기존 model 파싱에 영향이 없는지 확인한다.
- [x] role 값이 기존 프론트 enum/model과 호환되는지 확인한다.
- [x] 날짜 필드가 기존 ISO 8601 문자열 파싱 흐름과 호환되는지 확인한다.

## Phase 2. SSE Streaming 계약 재검증

- [x] `ChatRemoteDataSource.streamMessage`가 기존 `/api/v1/chat/stream`을 호출하는지 확인한다.
- [x] query parameter 이름이 `content`로 유지되는지 확인한다.
- [x] `Accept: text/event-stream` header가 유지되는지 확인한다.
- [x] 백엔드 SSE event name이 기존 `chunk`, `done`으로 유지되는지 확인한다.
- [x] 기간 필터 적용 후에도 chunk payload를 기존처럼 문자열로 누적 표시할 수 있는지 확인한다.
- [x] `done` 수신 또는 stream 종료 시 기존 완료 처리 흐름이 유지되는지 확인한다.
- [x] 기간 조건으로 RAG 결과가 비어 있을 때도 fallback assistant 응답이 일반 chunk처럼 표시되는지 확인한다.
- [x] stream 중 백엔드 오류가 발생하면 기존 error state로 전환되는지 확인한다.

## Phase 3. Chat 입력 UX 점검

- [x] `최근 5시간 내의 알림 내역을 요약해줘` 입력이 UI에서 잘리지 않고 전송되는지 확인한다.
- [x] `지난 30분 동안 중요한 알림 알려줘` 입력이 기존 전송 버튼/엔터 동작으로 전송되는지 확인한다.
- [x] `오늘 받은 알림 요약해줘` 입력이 기존 chat bubble에 정상 표시되는지 확인한다.
- [x] `어제 GitHub 알림 정리해줘` 입력이 기존 chat bubble에 정상 표시되는지 확인한다.
- [x] 기간 표현을 위한 별도 필터 UI를 추가하지 않는다.
- [x] 미지원 기간 표현을 입력해도 프론트에서는 일반 질문과 동일하게 처리한다.

## Phase 4. 로컬 캐시 및 히스토리 영향도 점검

- [x] 기간 필터는 RAG 검색에만 적용되므로 로컬 chat cache schema 변경이 필요 없는지 확인한다.
- [x] user message가 기간 표현을 포함해도 local cache에 원문 그대로 저장되는지 확인한다.
- [x] assistant fallback 응답이 기존 assistant message와 동일하게 저장되는지 확인한다.
- [x] streaming 완료 후 최종 assistant 메시지가 기존 흐름대로 local cache에 반영되는지 확인한다.
- [x] remote history refresh 시 기간 필터 개선과 무관하게 기존 정렬/중복 제거 흐름이 유지되는지 확인한다.
- [x] 앱 재시작 후 기간 표현이 포함된 대화가 history에 정상 복원되는지 확인한다.

## Phase 5. 빈 결과 및 장애 상태 점검

- [x] 기간 조건에 맞는 RAG 결과가 없을 때 백엔드 fallback 응답이 일반 assistant 메시지처럼 표시되는지 확인한다.
- [x] 기간 파싱 실패 fallback 상황이 프론트 error state로 표시되지 않는지 확인한다.
- [x] LLM 또는 embedding 장애 시 기존 백엔드 표준 error message가 표시되는지 확인한다.
- [x] Ollama 미기동 또는 backend timeout 시 입력창이 다시 활성화되는지 확인한다.
- [x] SSE 연결이 중간에 끊겨도 `isSending`, `isStreaming` 상태가 정상 해제되는지 확인한다.
- [x] retry 또는 재전송 시 user message가 의도치 않게 중복 저장되지 않는지 확인한다.

## Phase 6. 프론트 변경 필요 조건

- [x] 백엔드가 Chat API path를 변경하면 `ApiConstants`와 datasource를 수정한다.
- [x] 백엔드가 request field를 `content`가 아닌 이름으로 변경하면 `ChatRequest`를 수정한다.
- [x] 백엔드가 response field를 변경하면 `ChatMessageModel` 파서를 수정한다.
- [x] 백엔드가 SSE payload를 JSON event type 기반으로 변경하면 stream parser를 수정한다.
- [x] 백엔드가 기간 필터 정보를 응답에 노출하기로 결정하면 model과 UI 표시 여부를 별도 검토한다.
- [x] 백엔드가 인증 사용자 기준 chat scope를 강제하면 Dio auth header 흐름을 재검증한다.

## Phase 7. 자동 검증

- [x] `flutter analyze`를 실행한다.
- [x] `flutter test`를 실행한다.
- [x] `chat_remote_datasource` 관련 테스트가 있으면 기존 API 계약 기준으로 통과하는지 확인한다.
- [x] `chat_repository` 관련 테스트가 local cache/history 흐름을 유지하는지 확인한다.
- [x] SSE parser 테스트가 있으면 `chunk`, `done` 형식을 유지하는지 확인한다.

## Phase 8. 수동 통합 검증

- [ ] 백엔드 서버와 Flutter 앱을 함께 실행한다.
- [ ] Chat 화면에서 `최근 5시간 내의 알림 내역을 요약해줘`를 전송해 응답이 표시되는지 확인한다.
- [ ] Chat 화면에서 `지난 30분 동안 중요한 알림 알려줘`를 전송해 streaming 응답이 표시되는지 확인한다.
- [ ] Chat 화면에서 `오늘 받은 알림 요약해줘`를 전송해 기존 UI 흐름이 유지되는지 확인한다.
- [ ] Chat 화면에서 기간 표현 없는 일반 질문을 전송해 기존 RAG 검색과 동일하게 동작하는지 확인한다.
- [ ] 미지원 표현인 `4월 20일부터 4월 22일까지 알림 요약해줘`를 전송해 프론트 오류 없이 fallback 응답이 표시되는지 확인한다.
- [ ] 앱 재시작 후 기간 표현이 포함된 chat history가 정상 표시되는지 확인한다.

## Phase 9. 완료 기준

- [ ] 프론트 코드 변경 없이 기존 Chat 화면에서 자연어 기간 질문을 전송할 수 있다.
- [ ] 기존 `POST /api/v1/chat` 호출이 정상 동작한다.
- [ ] 기존 `GET /api/v1/chat/stream?content=...` 호출이 정상 동작한다.
- [ ] 기존 SSE event 형식 `chunk`, `done` 처리에 회귀가 없다.
- [ ] 기간 조건 결과 없음, 기간 파싱 실패, LLM 장애가 기존 UI 상태로 처리된다.
- [ ] `flutter analyze`와 `flutter test`가 통과하거나, 실행 불가 사유가 문서화된다.
