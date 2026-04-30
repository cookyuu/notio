# Backend 운영 로그 및 관측성 개발 체크리스트

> 기준 문서: `docs/plans/plan_backend_log.md`  
> 대상: `backend` 전반의 운영 로그 표준화, correlation id 도입, Prometheus + Grafana 연동 준비  
> 범위: 현재 Spring Boot 모놀리스 기준 구현을 우선하고, 이후 API Gateway / Kafka event 구조로 확장 가능한 형태를 설계·반영

---

## Phase 0. 범위 확정 및 운영 원칙 고정

- [x] `docs/plans/plan_backend_log.md`를 기준 문서로 확정한다.
- [x] 현재 단계의 공통 식별자를 `correlation_id` 하나로 통일한다.
- [x] `request_id`, `transaction_id`를 별도 도입하지 않고 `correlation_id`에 역할을 집중한다.
- [x] `correlation_id`는 모든 `/api/v1/**` 요청에 대해 반드시 존재하도록 한다.
- [x] `X-Correlation-Id` 요청 헤더가 있으면 재사용하고 없으면 서버가 생성하도록 규칙을 고정한다.
- [x] 응답 헤더에도 동일한 `X-Correlation-Id`를 반환하도록 한다.
- [x] `prod`는 JSON 구조화 로그, `local/dev`는 텍스트 로그를 사용하되 MDC 필드는 동일하게 유지한다.
- [x] 민감정보 비로그 정책을 구현 기준으로 고정한다.
- [x] 이번 작업에서 비즈니스 API 계약은 변경하지 않는다.
- [x] 이번 작업에서 우선 구현 범위는 Phase A(모놀리스 기준)까지로 두고, Phase B/C는 후속 작업용 체크리스트로 관리한다.

상세 가이드:
- `correlation_id`는 운영자가 특정 요청 하나를 처음부터 끝까지 검색하기 위한 기준값이다.
- 현재 단계에서는 외부 tracing이 없으므로 `trace_id` 대신 `correlation_id` 중심으로 로그를 설계한다.
- 민감정보 비로그 대상은 최소 다음을 포함한다.
  - Authorization header
  - access token / refresh token / JWT 원문
  - API key / webhook secret
  - FCM token
  - 비밀번호 원문
  - raw request body 전체
- 허용되는 대체 정보는 길이, source, provider, id 계열 식별자, count, outcome 수준으로 제한한다.

검증 메모:
- 기준 문서와 실제 구현 범위가 어긋나지 않는지 `docs/plans/plan_backend_log.md`와 `docs/tasks/task_backend_fix.md`를 함께 검토한다.
- `backend/src/main/resources/application*.yml`, `SecurityConfig`, 공통 filter, logback 설정에서 상위 원칙이 일관되게 반영되는지 확인한다.

---

## Phase 1. 의존성 및 설정 기반 추가

- [x] `backend/build.gradle.kts`에 `spring-boot-starter-actuator`를 추가한다.
- [x] `backend/build.gradle.kts`에 `micrometer-registry-prometheus`를 추가한다.
- [x] `application.yml`에 `management.endpoints.web.exposure.include=health,info,prometheus,metrics`를 추가한다.
- [x] `application.yml`에 `management.endpoint.health.show-details=never`를 추가한다.
- [x] `application.yml`에 `management.prometheus.metrics.export.enabled=true`를 추가한다.
- [x] `application-local.yml`과 `application-prod.yml`의 logging/management 정책이 상위 설정과 충돌하지 않도록 정리한다.
- [x] `SecurityConfig`에서 `/actuator/health`를 인증 없이 허용한다.
- [x] `SecurityConfig`에서 `/actuator/prometheus`를 인증 없이 허용한다.
- [x] 기존 인증/웹훅/public auth endpoint 정책이 actuator 추가로 깨지지 않게 유지한다.

상세 가이드:
- management port는 이번 작업에서 분리하지 않고 애플리케이션 동일 포트에서 노출한다.
- `/actuator/prometheus`와 `/actuator/health`만 공개 대상이며, 나머지 actuator endpoint를 불필요하게 열지 않는다.
- Prometheus 수집 기준은 pull 모델이며, custom metrics는 이후 service 계층에서 meter로 추가한다.

검증 메모:
- `SecurityConfigTest`, `AuthPublicEndpointSecurityTest` 성격의 테스트를 기준으로 actuator 공개 정책을 검증한다.
- 수동 확인 시 `/actuator/health`, `/actuator/prometheus`가 200 응답이고 인증이 필요 없는지 확인한다.

---

## Phase 2. 공통 correlation filter 및 MDC 표준화

- [x] `/api/v1/**` 요청에 적용되는 공통 `OncePerRequestFilter`를 추가한다.
- [x] 요청 헤더 `X-Correlation-Id`가 있으면 해당 값을 그대로 사용한다.
- [x] 요청 헤더가 없으면 서버가 새로운 `correlation_id`를 생성한다.
- [x] 생성 또는 재사용한 `correlation_id`를 MDC에 저장한다.
- [x] `route`, `http_method`를 MDC 또는 공통 로그 필드로 저장한다.
- [x] 응답 헤더에 `X-Correlation-Id`를 설정한다.
- [x] 요청 시작 시 `event=request_started` 로그를 남긴다.
- [x] 요청 완료 시 `event=request_completed` 로그를 남긴다.
- [x] 완료 로그에 `status`, `elapsed_ms`, `authenticated`를 포함한다.
- [x] 예외 발생 여부와 관계없이 `finally`에서 MDC를 정리한다.
- [x] `/api/v1/**` 외 경로에 대한 필터 적용 범위를 명확히 제한한다.

상세 가이드:
- filter 이름은 `RequestCorrelationFilter` 또는 같은 의도를 명확히 드러내는 이름으로 둔다.
- 생성값은 정렬 가능한 ULID 또는 UUIDv7 계열을 우선 사용한다. 라이브러리 추가가 과하면 우선 UUID를 사용하되 생성 형식은 한 곳에 캡슐화한다.
- 시작 로그에는 body를 찍지 않고 최소 다음만 남긴다.
  - `event=request_started`
  - `correlation_id`
  - `route`
  - `http_method`
  - `client_ip`
  - `user_agent`
- 완료 로그에는 최소 다음을 남긴다.
  - `event=request_completed`
  - `correlation_id`
  - `route`
  - `http_method`
  - `status`
  - `elapsed_ms`
  - `authenticated`

검증 메모:
- 새 filter 단위 테스트를 추가해 헤더 재사용, 헤더 미존재 시 생성, 응답 헤더 반영, MDC 정리 여부를 검증한다.
- MockMvc 또는 filter 테스트에서 요청 한 건의 시작/완료 로그가 같은 `correlation_id`를 공유하는지 확인한다.

---

## Phase 3. logback 및 로그 포맷 표준화

- [x] `logback-spring.xml`을 추가한다.
- [x] `prod` 프로필에서 JSON 구조화 로그를 출력하도록 설정한다.
- [x] `local/dev` 프로필에서 사람이 읽기 쉬운 텍스트 로그를 출력하도록 설정한다.
- [x] 두 포맷 모두 `correlation_id`를 포함한다.
- [x] 두 포맷 모두 `event`, `outcome`, `route`, `http_method` 등 핵심 필드를 포함한다.
- [x] stacktrace는 `ERROR`/`WARN` 정책에 맞게 JSON에서도 읽기 가능한 필드로 남긴다.
- [x] logger level 설정이 `application.yml`, `application-local.yml`, `application-prod.yml`과 충돌하지 않게 정리한다.

상세 가이드:
- `prod` JSON 구조화 로그는 Grafana/Loki/Promtail 계열 적재를 전제로 한다.
- 공통 필드는 최소 다음을 포함한다.
  - `timestamp`
  - `level`
  - `logger`
  - `thread`
  - `message`
  - `correlation_id`
  - `event`
  - `outcome`
- 텍스트 로그도 최소한 `correlation_id`가 한눈에 보이도록 pattern을 구성한다.
- 메시지는 자유 문장보다 `event=...` 중심으로 정규화한다.

검증 메모:
- local/dev 실행 시 텍스트 로그에 `correlation_id`가 노출되는지 확인한다.
- prod profile 실행 시 JSON 필드에 `correlation_id`와 `event`가 포함되는지 확인한다.

---

## Phase 4. HTTP / Security / Exception 로그 정규화

- [x] `JwtAuthenticationFilter`의 성공/실패 로그를 표준 필드 체계에 맞게 정리한다.
- [x] JWT 인증 성공은 `DEBUG`로 유지한다.
- [x] JWT 인증 실패는 `WARN`으로 표준화한다.
- [x] JWT 실패 로그에 `reason`, `route`, `correlation_id`를 포함한다.
- [x] `RateLimitFilter` 차단 시 `event=rate_limit_blocked` 로그를 남긴다.
- [x] rate limit 차단 로그에 `limit`, `remaining`, `reset_at`, `route`를 포함한다.
- [x] rate limit store 장애 시 `ERROR` 로그를 남긴다.
- [x] `GlobalExceptionHandler`의 `NotioException` 로그를 `WARN`으로 정리한다.
- [x] validation / bad request 로그를 `WARN`으로 정리한다.
- [x] 예기치 못한 예외 로그를 `ERROR`로 정리한다.
- [x] 예외 로그에 `event=request_failed`, `correlation_id`, `error_code`, `http_status`, `exception_type`, `root_cause`를 포함한다.

상세 가이드:
- 보안 관련 로그는 민감정보를 직접 출력하지 않고 실패 사유의 category만 남긴다.
- `GlobalExceptionHandler`는 예외 응답 shape를 바꾸는 것이 아니라, 같은 응답을 유지한 채 운영 로그 필드만 정규화한다.
- `NotioException`은 정상적인 비즈니스 예외 경로이므로 stacktrace 남발을 피하고, generic exception만 전체 stacktrace를 우선 기록한다.

검증 메모:
- `GlobalExceptionHandler` 관련 controller test 또는 slice test에서 예외 시 로그 필드가 누락되지 않는지 확인한다.
- rate limit / JWT filter test에서 실패 분기 로그와 기존 응답 형식이 모두 유지되는지 확인한다.

---

## Phase 5. Auth 도메인 로그 표준화

- [x] `AuthService`에 로그인 성공/실패 로그를 추가 또는 정리한다.
- [x] `AuthService`에 토큰 재발급 성공/실패 로그를 추가 또는 정리한다.
- [x] `OAuthAuthService`에 OAuth start/callback/exchange 로그를 표준화한다.
- [x] `LocalAuthService`의 signup/find-id/password-reset 흐름 로그를 표준화한다.
- [x] `AuthAuditService`의 이벤트명과 서비스 로그의 vocabulary를 맞춘다.
- [x] auth 로그에 `provider`, `platform`, `user_id`, `masked_email`, `outcome`을 필요한 범위에서 포함한다.
- [x] email은 반드시 마스킹된 값만 로그에 남긴다.
- [x] token, code, state 원문을 로그에 남기지 않는다.

상세 가이드:
- auth 로그 이벤트명은 다음 기준으로 통일한다.
  - `auth_login_succeeded`
  - `auth_login_failed`
  - `auth_refresh_succeeded`
  - `auth_refresh_failed`
  - `oauth_start`
  - `oauth_callback_validated`
  - `oauth_exchange_succeeded`
  - `oauth_exchange_failed`
- 실패 로그에는 `error_code` 또는 `reason_category` 수준의 정보만 남긴다.

검증 메모:
- auth 관련 service test에서 민감값 미노출과 성공/실패 로그 분기를 함께 점검한다.
- 공용 auth endpoint security 테스트와 충돌하지 않는지 확인한다.

---

## Phase 6. Chat / LLM / RAG / SSE 로그 및 메트릭

- [x] `ChatService.chat()` 시작 시 `event=chat_request_started` 로그를 남긴다.
- [x] `ChatService.streamChat()` 시작 시 `event=chat_stream_started` 로그를 남긴다.
- [x] chat 로그에 `correlation_id` 외에 `stream_id`, `time_range_applied`, `history_count`를 포함한다.
- [x] prompt 생성 완료 시 `event=chat_prompt_built` 로그를 남긴다.
- [x] prompt 관련 로그에는 `rag_result_count`, `prompt_chars`를 포함한다.
- [x] first chunk 수신 시 `event=chat_stream_first_chunk` 로그를 남긴다.
- [x] stream 완료 시 `event=chat_stream_completed` 로그를 남긴다.
- [x] stream cancel/timeout/failure 시 각각 별도 event로 로그를 남긴다.
- [x] `OllamaLlmProvider`에 `llm_call_started`, `llm_call_completed`, `llm_call_failed` 로그를 추가한다.
- [x] `OllamaLlmProvider` 로그에 `mode`, `timeout_ms`, `elapsed_ms`, `exception_type`를 포함한다.
- [x] `PgvectorRagRetriever`에 `event=rag_retrieve_completed` 로그를 추가한다.
- [x] RAG 로그에 `time_range_applied`, `top_k`, `result_count`, `elapsed_ms`를 포함한다.
- [x] chat 관련 Prometheus counter/timer/gauge를 추가한다.

상세 가이드:
- chunk 본문 전체를 로그에 찍지 않는다. `chunk_count`, `response_chars`, `first_chunk_elapsed_ms` 위주로 남긴다.
- prompt 원문은 미출력하고 `prompt_chars` 또는 `context_count` 수준만 기록한다.
- 최소 메트릭:
  - `notio_chat_requests_total{mode,outcome}`
  - `notio_chat_duration{mode}`
  - `notio_chat_first_chunk_duration`
  - `notio_llm_call_duration{mode}`
  - `notio_rag_retrieval_total{time_range_applied}`
  - `notio_rag_retrieval_duration`
  - `notio_chat_stream_active`
  - `notio_chat_response_chars`

검증 메모:
- `ChatServiceTest`, `PromptBuilderTest`, `PgvectorRagRetrieverTest` 기반 테스트를 확장해 로그/metrics hook이 흐름을 깨지 않는지 확인한다.
- stream chat에서 동일 `correlation_id`와 동일 `stream_id`로 시작/first chunk/done/failure 로그를 조회할 수 있는지 확인한다.

---

## Phase 7. Webhook / Connection / Notification / Push 로그 및 메트릭

- [x] `WebhookController`에 `event=webhook_received` 로그를 추가한다.
- [x] webhook 수신 로그에 `source`, `provider`, `payload_size`, `correlation_id`를 포함한다.
- [x] `WebhookDispatcher`에 인증 완료 로그 `event=webhook_authenticated`를 추가한다.
- [x] `WebhookDispatcher` 또는 변환 계층에 `event=webhook_event_mapped` 로그를 추가한다.
- [x] webhook 처리 완료 시 `event=webhook_processed` 로그를 남긴다.
- [x] webhook 거부/검증 실패 시 `event=webhook_rejected` 로그를 남긴다.
- [x] `ConnectionService`의 이벤트 저장 vocabulary와 운영 로그 vocabulary를 맞춘다.
- [x] `NotificationService`의 생성 성공 로그를 `event=notification_created` 기준으로 정리한다.
- [x] notification embedding 실패 로그를 `event=notification_embedding_failed`로 정리한다.
- [x] push 발송 시작/성공/실패 로그를 표준화한다.
- [x] `PushService`와 `DeviceService` 로그에서 FCM token을 제거한다.
- [x] webhook / notification / push 관련 Prometheus counter/timer를 추가한다.

상세 가이드:
- webhook 계층은 하나의 `correlation_id`로 `receive -> authenticate -> map -> save -> push`까지 추적할 수 있어야 한다.
- notification 생성 성공 로그의 필드는 최소 다음을 포함한다.
  - `notification_id`
  - `user_id`
  - `connection_id`
  - `source`
  - `outcome`
- push 로그는 최소 다음을 포함한다.
  - `notification_id`
  - `device_count`
  - `success_count`
  - `failure_count`
- 최소 메트릭:
  - `notio_webhook_requests_total{source,outcome}`
  - `notio_webhook_processing_duration{source}`
  - `notio_notifications_created_total{source}`
  - `notio_notification_embedding_total{outcome}`
  - `notio_push_send_total{outcome}`
  - `notio_push_send_duration`

검증 메모:
- `WebhookControllerTest`, `WebhookDispatcherTest`, `NotificationServiceTest`를 기준으로 정상/실패 분기 로그와 메트릭 증가를 점검한다.
- webhook 처리 중 인증 실패, notification 저장 성공, embedding 실패, push 실패가 서로 다른 event/outcome으로 남는지 확인한다.

---

## Phase 8. Prometheus 메트릭 계층 정리

- [ ] 공용 메트릭 등록 방식을 정한다.
- [ ] `MeterRegistry`를 직접 주입할지, 별도 metrics helper/component를 둘지 패턴을 고정한다.
- [ ] HTTP 기본 메트릭(`http.server.requests`)이 유지되는지 확인한다.
- [ ] 도메인 메트릭 이름 규칙을 `notio_*` prefix로 통일한다.
- [ ] counter/timer/gauge 이름과 tag 체계를 문서 기준으로 고정한다.
- [ ] 메트릭 tag에 `correlation_id`, `user_id`, `stream_id`를 넣지 않도록 방지한다.
- [ ] 허용 tag는 `route`, `provider`, `source`, `mode`, `outcome`, `exception` 수준으로 제한한다.
- [ ] 메트릭 생성이 비즈니스 로직을 오염시키지 않도록 helper layer를 두거나 호출 위치를 최소화한다.

상세 가이드:
- 고카디널리티 값은 Prometheus 비용을 크게 올리므로 절대 tag로 넣지 않는다.
- 로그 검색용 식별자와 메트릭 집계용 태그는 용도가 다르므로 분리한다.
- 메트릭 명명은 snake_case 또는 dot 혼용 없이 일관되게 `notio_xxx_total`, `notio_xxx_duration` 형태를 유지한다.

검증 메모:
- `/actuator/prometheus` 출력에서 커스텀 메트릭이 기대 이름과 tag 조합으로 노출되는지 확인한다.
- tag cardinality가 높아질 수 있는 필드가 실수로 들어가지 않았는지 점검한다.

---

## Phase 9. 테스트 보강

- [ ] correlation filter 단위 테스트를 추가한다.
- [ ] actuator endpoint 공개 여부에 대한 security 테스트를 추가한다.
- [ ] `GlobalExceptionHandler` 구조화 로그 테스트를 추가한다.
- [ ] auth 서비스 로그 정책에 대한 테스트를 보강한다.
- [ ] chat stream 시작/first chunk/done/failure correlation 일관성 테스트를 보강한다.
- [ ] webhook receive/authenticate/process correlation 일관성 테스트를 보강한다.
- [ ] 민감정보 비로그 정책 테스트를 추가한다.
- [ ] Prometheus 메트릭 노출 및 증가 검증 테스트를 추가한다.

상세 가이드:
- 로그 자체를 완전 문자열 비교하기보다, appender 캡처 또는 logging test utility로 핵심 필드 존재 여부를 검증한다.
- 메트릭 테스트는 registry를 주입받아 counter/timer 값 증가 여부를 직접 검증한다.
- 민감정보 테스트는 실제 토큰/키/헤더 값이 로그에 포함되지 않았음을 부정 검증으로 확인한다.

검증 메모:
- 대상 테스트 후보:
  - `SecurityConfigTest`
  - `AuthPublicEndpointSecurityTest`
  - `RateLimitFilterTest`
  - `ChatServiceTest`
  - `WebhookControllerTest`
  - `NotificationServiceTest`

---

## Phase 10. 운영 검증

- [ ] `./gradlew test`를 실행한다.
- [ ] 애플리케이션 실행 후 `/actuator/health`가 정상 응답하는지 확인한다.
- [ ] 애플리케이션 실행 후 `/actuator/prometheus`가 정상 응답하는지 확인한다.
- [ ] 일반 API 요청 1건에 대해 시작/완료/예외 로그가 같은 `correlation_id`를 공유하는지 확인한다.
- [ ] auth API 요청 시 성공/실패 로그가 표준 event/outcome 체계를 따르는지 확인한다.
- [ ] chat sync 요청 시 `chat_request_started -> rag_retrieve_completed -> llm_call_completed` 흐름이 같은 `correlation_id`로 조회되는지 확인한다.
- [ ] chat stream 요청 시 `chat_stream_started -> chat_stream_first_chunk -> chat_stream_completed` 흐름이 같은 `correlation_id`와 `stream_id`로 조회되는지 확인한다.
- [ ] webhook 요청 시 `webhook_received -> webhook_authenticated -> webhook_processed -> notification_created` 흐름이 같은 `correlation_id`로 조회되는지 확인한다.
- [ ] notification 생성 후 push 실패 시 요청 전체는 성공 처리되되 `push_dispatch_failed` 로그와 관련 메트릭이 남는지 확인한다.
- [ ] 로그에 민감정보가 포함되지 않는지 샘플링 검토한다.
- [ ] Grafana/Loki 기준으로 `correlation_id` 검색이 가능한 로그 필드 구조인지 확인한다.

상세 가이드:
- 운영 검증은 local 또는 dev profile에서 텍스트 로그 가독성을 먼저 확인하고, prod profile에서 JSON 구조를 확인하는 순서가 좋다.
- Prometheus scrape 결과는 텍스트 노출만 보는 것이 아니라, custom metric이 실제 요청 후 증가했는지도 함께 확인한다.
- webhook/chat/auth는 서로 성격이 달라 대표 시나리오로 각각 1개 이상 확인한다.

검증 메모:
- 대표 수동 검증 시나리오:
  - `POST /api/v1/auth/login`
  - `POST /api/v1/chat`
  - `GET /api/v1/chat/stream?content=...`
  - `POST /api/v1/webhook/{source}`

---

## Phase 11. API Gateway 도입 후 후속 체크리스트

- [ ] Gateway가 `traceparent`를 생성/전파하는지 정책을 확정한다.
- [ ] Gateway가 `X-Correlation-Id`를 생성 또는 전달하는지 정책을 확정한다.
- [ ] backend가 `traceparent`를 우선 수용하고 `X-Correlation-Id`를 보조 식별자로 유지하도록 변경한다.
- [ ] 로그 필드에 `trace_id`, `span_id`, `correlation_id`를 함께 노출한다.
- [ ] tracing 계층이 없을 때의 fallback 생성 규칙을 정리한다.
- [ ] Gateway에서 온 correlation 값과 backend fallback 값의 충돌 정책을 명확히 한다.

상세 가이드:
- 이 phase에서는 서비스 코드가 `trace_id`를 직접 생성하지 않도록 해야 한다.
- `trace_id`는 기술적 호출 체인, `correlation_id`는 비즈니스 흐름/운영 검색용 역할을 유지한다.

검증 메모:
- Gateway가 넣은 헤더가 backend 로그에 그대로 반영되는지 end-to-end로 점검한다.

---

## Phase 12. Kafka event 도입 후 후속 체크리스트

- [ ] Kafka producer header 표준(`traceparent`, `x-correlation-id`, `event-id`, `causation-id`, `source-service`, `event-type`, `event-version`)을 확정한다.
- [ ] HTTP 요청에서 시작된 첫 이벤트가 기존 `correlation_id`를 재사용하도록 한다.
- [ ] 각 Kafka 메시지에 새 `event_id`를 생성한다.
- [ ] 후속 이벤트 발행 시 이전 `event_id`를 `causation_id`로 전달한다.
- [ ] consumer가 Kafka header를 읽어 MDC를 복원하도록 한다.
- [ ] consumer 로그에 `trace_id`, `correlation_id`, `event_id`, `causation_id`, `topic`, `partition`, `offset`, `event_type`, `outcome`을 포함한다.
- [ ] Kafka publish/consume/event processing 메트릭을 추가한다.
- [ ] 메트릭 tag에 `event_id`, `trace_id`, `correlation_id`를 넣지 않도록 제한한다.

상세 가이드:
- Kafka 도입 후에는 HTTP 요청 하나가 여러 비동기 처리로 분기되므로, `correlation_id`는 비즈니스 흐름 복원용으로 매우 중요하다.
- `event_id`는 개별 메시지 식별자, `causation_id`는 이벤트 체인 연결용으로 사용한다.

검증 메모:
- ingress -> producer -> consumer -> 후속 producer 흐름을 같은 `correlation_id`로 조회할 수 있는지 검증한다.
