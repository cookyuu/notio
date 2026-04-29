# Backend 운영 로그 및 관측성 설계 계획

> 대상: `backend` 전반의 운영 로그 표준화 및 Prometheus + Grafana 연동 준비  
> 기준: 현재 Spring Boot 모놀리스 구조 유지, 추후 API Gateway 및 Kafka event 확장 가능성 반영

---

## 1. 목표

현재 `backend`에는 서비스별 개별 로그는 일부 존재하지만, 운영 관점에서 다음 문제가 있다.

- 요청 단위로 로그를 끝까지 묶어 보기 어렵다.
- 서비스마다 로그 메시지와 필드가 제각각이다.
- Prometheus가 바로 수집할 수 있는 표준 메트릭 계층이 없다.
- Grafana에서 로그와 메트릭을 같은 기준으로 연결해 보기 어렵다.

이번 설계의 목표는 다음과 같다.

- 모든 주요 요청에 대해 공통 식별자인 `correlation_id`를 부여한다.
- 중요 프로세스마다 일정한 필드 체계로 구조화 로그를 남긴다.
- `/actuator/prometheus`를 통해 Prometheus 수집이 가능한 메트릭을 제공한다.
- 현재 모놀리스 구조에서 바로 적용할 수 있으면서, 이후 API Gateway와 Kafka event 구조로 자연스럽게 확장 가능하게 한다.

---

## 2. 핵심 원칙

### 2.1 현재 단계 기준 식별자

현재 단계에서는 `request_id`와 `transaction_id`를 별도로 두지 않고 `correlation_id` 하나로 통일한다.

원칙:

- 모든 `/api/v1/**` 요청은 반드시 하나의 `correlation_id`를 가진다.
- 요청 헤더 `X-Correlation-Id`가 있으면 재사용한다.
- 없으면 서버에서 새로 생성한다.
- 응답 헤더에도 같은 값을 반환한다.

### 2.2 장기 확장 방향

추후 API Gateway와 Kafka를 도입하면 식별자를 다음처럼 확장한다.

- `trace_id`: 기술적 호출 체인 추적
- `span_id`: trace 내부 구간 식별
- `correlation_id`: 비즈니스 흐름 추적
- `event_id`: Kafka 개별 메시지 식별
- `causation_id`: 상위 이벤트 연계 식별

즉 현재는 `correlation_id` 중심으로 시작하고, 장기적으로는 `trace_id + correlation_id` 조합으로 간다.

### 2.3 로그 포맷

- `prod`: JSON 구조화 로그
- `local/dev`: 텍스트 로그

단, 두 환경 모두 동일한 MDC 필드를 유지한다.

### 2.4 민감정보 정책

다음 값은 로그에 남기지 않는다.

- Authorization header
- access token / refresh token / JWT 원문
- API key / webhook secret
- FCM token
- 비밀번호 원문
- raw request body 전체

필요한 경우에는 길이, source, provider, id 계열 식별자만 기록한다.

### 2.5 이번 작업의 범위와 비목표

- 이번 작업은 비즈니스 API 계약을 변경하지 않는다.
- 현재 구현 우선순위는 Phase A. 현재 모놀리스 기준 설계와 구현까지로 제한한다.
- Phase B, Phase C는 API Gateway, Kafka event, tracing 확장 시점에 후속 체크리스트로 관리한다.
- 이번 단계의 운영 로그 기준은 `correlation_id` 하나를 중심으로 고정한다.

---

## 3. Phase A. 현재 모놀리스 기준 설계

### 3.1 공통 correlation filter

`OncePerRequestFilter` 기반의 공통 요청 상관관계 필터를 추가한다.

책임:

- `/api/v1/**` 요청에 대해 `correlation_id`를 생성 또는 재사용한다.
- `MDC`에 `correlation_id`, `route`, `http_method` 등을 넣는다.
- 응답 헤더 `X-Correlation-Id`를 설정한다.
- 요청 시작/완료 로그를 남긴다.
- 요청 종료 후 `MDC`를 정리한다.

우선순위:

1. `X-Correlation-Id` 헤더가 있으면 그대로 사용
2. 없으면 서버가 ULID 또는 UUIDv7 계열 문자열 생성

요청 시작 로그 예시 필드:

- `event=request_started`
- `correlation_id`
- `route`
- `http_method`
- `client_ip`
- `user_agent`

요청 완료 로그 예시 필드:

- `event=request_completed`
- `correlation_id`
- `route`
- `http_method`
- `status`
- `elapsed_ms`
- `authenticated`

### 3.2 공통 로그 필드 표준

운영 로그의 공통 필드는 최소 다음으로 통일한다.

- `correlation_id`
- `event`
- `outcome`
- `route`
- `http_method`
- `user_id` (있을 때만)
- `error_code` (실패 시)
- `elapsed_ms` (처리 시간 측정이 필요한 경우)

도메인별 보조 필드:

- chat: `stream_id`, `time_range_applied`, `rag_result_count`
- webhook: `provider`, `source`, `connection_id`, `payload_size`
- notification: `notification_id`
- auth: `provider`, `platform`, `masked_email`
- push: `device_count`, `success_count`, `failure_count`

### 3.3 로그 대상 프로세스

#### HTTP / Security / Exception

- `JwtAuthenticationFilter`
  - JWT 인증 성공은 `DEBUG`
  - JWT 검증 실패는 `WARN`
  - `reason`, `route`, `correlation_id` 기록
- `RateLimitFilter`
  - 차단 시 `WARN`
  - `event=rate_limit_blocked`
  - `limit`, `remaining`, `reset_at`, `route`
- `GlobalExceptionHandler`
  - `NotioException`: `WARN`
  - validation / bad request: `WARN`
  - 예상치 못한 예외: `ERROR`
  - 공통 필드:
    - `event=request_failed`
    - `correlation_id`
    - `error_code`
    - `http_status`
    - `exception_type`
    - `root_cause`

#### Auth

대상:

- `AuthService`
- `OAuthAuthService`
- `LocalAuthService`
- `AuthAuditService`

로그 이벤트:

- `auth_login_succeeded`
- `auth_login_failed`
- `auth_refresh_succeeded`
- `auth_refresh_failed`
- `oauth_start`
- `oauth_callback_validated`
- `oauth_exchange_succeeded`
- `oauth_exchange_failed`

정책:

- email은 마스킹된 값만 출력
- token, code, state 원문은 로그 금지

#### Chat / LLM / RAG / SSE

대상:

- `ChatService`
- `OllamaLlmProvider`
- `PgvectorRagRetriever`
- `PromptBuilder` 관련 흐름

로그 이벤트:

- `chat_request_started`
- `chat_prompt_built`
- `rag_retrieve_completed`
- `llm_call_started`
- `llm_call_completed`
- `llm_call_failed`
- `chat_stream_started`
- `chat_stream_first_chunk`
- `chat_stream_completed`
- `chat_stream_cancelled`
- `chat_stream_timeout`
- `chat_stream_failed`

추가 필드:

- `stream_id`
- `time_range_applied`
- `rag_result_count`
- `history_count`
- `prompt_chars`
- `first_chunk_elapsed_ms`
- `response_chars`
- `chunk_count`

#### Webhook / Connection / Notification / Push

대상:

- `WebhookController`
- `WebhookDispatcher`
- `ConnectionService`
- `NotificationService`
- `PushService`
- `DeviceService`

로그 이벤트:

- `webhook_received`
- `webhook_authenticated`
- `webhook_event_mapped`
- `webhook_processed`
- `webhook_rejected`
- `notification_created`
- `notification_embedding_failed`
- `push_dispatch_started`
- `push_dispatch_succeeded`
- `push_dispatch_failed`
- `device_registered`
- `device_deactivated`

Webhook 관련 필드:

- `source`
- `provider`
- `connection_id`
- `user_id`
- `payload_size`
- `notification_id`

Push 관련 필드:

- `notification_id`
- `device_count`
- `success_count`
- `failure_count`

### 3.4 Prometheus 메트릭

`backend/build.gradle.kts`에 다음 의존성을 추가한다.

- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

`application.yml`에 management 설정을 추가한다.

예시:

```yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: never
  prometheus:
    metrics:
      export:
        enabled: true
```

`SecurityConfig`에서는 다음 엔드포인트를 공개 허용한다.

- `/actuator/health`
- `/actuator/prometheus`

기본 HTTP 메트릭은 유지하고, 다음 커스텀 메트릭을 추가한다.

#### Counter

- `notio_auth_login_total{outcome}`
- `notio_auth_refresh_total{outcome}`
- `notio_oauth_exchange_total{provider,outcome}`
- `notio_webhook_requests_total{source,outcome}`
- `notio_notifications_created_total{source}`
- `notio_notification_embedding_total{outcome}`
- `notio_push_send_total{outcome}`
- `notio_chat_requests_total{mode,outcome}`
- `notio_rag_retrieval_total{time_range_applied}`
- `notio_rate_limit_total{route,outcome}`

#### Timer

- `notio_chat_duration{mode}`
- `notio_chat_first_chunk_duration`
- `notio_llm_call_duration{mode}`
- `notio_rag_retrieval_duration`
- `notio_webhook_processing_duration{source}`
- `notio_notification_save_duration`
- `notio_push_send_duration`

#### Gauge / Distribution

- `notio_chat_stream_active`
- `notio_chat_response_chars`
- `notio_webhook_payload_bytes`
- `notio_rag_results_count`

제약:

- 메트릭 태그에 `correlation_id`, `user_id`, `stream_id`는 넣지 않는다.
- 허용 태그는 `route`, `provider`, `source`, `mode`, `outcome`, `exception` 수준으로 제한한다.

### 3.5 Grafana 기준 대시보드

초기 Grafana 운영 기준 패널은 다음을 상정한다.

- API latency / error rate
- webhook success rate by source
- chat stream first-chunk latency
- chat timeout / failure count
- auth login failure rate
- push success / failure rate
- rate limit block count
- embedding / LLM failure trend

---

## 4. Phase B. API Gateway 도입 후 설계

### 4.1 Gateway 기준 식별자 전환

API Gateway 도입 후에는 ingress의 기준점을 Gateway로 둔다.

Gateway가 가능하면 다음을 생성하거나 전달한다.

- `traceparent`
- `tracestate`
- `X-Correlation-Id`

backend의 규칙은 다음으로 전환한다.

1. `traceparent`가 있으면 trace context를 우선 수용
2. `X-Correlation-Id`가 있으면 재사용
3. 둘 다 없으면 fallback 생성

### 4.2 로그 필드 확장

이 단계부터 로그 필드는 다음으로 확장한다.

- `trace_id`
- `span_id`
- `correlation_id`

원칙:

- `trace_id`: 기술적 호출 체인
- `correlation_id`: 비즈니스 흐름 또는 운영 검색용 식별자

### 4.3 애플리케이션 처리 규칙

- 서비스 코드에서 `trace_id`를 직접 생성하지 않는다.
- tracing 계층 또는 공통 관측성 계층에서 `trace_id`, `span_id`를 MDC에 넣는다.
- backend는 `correlation_id`를 계속 유지한다.
- 응답 헤더에는 최소 `X-Correlation-Id`를 유지한다.

### 4.4 운영 기준

이 단계에서는 Grafana에서 다음 두 축으로 조회할 수 있어야 한다.

- `trace_id` 기반 분산 호출 분석
- `correlation_id` 기반 비즈니스 흐름 및 장애 티켓 분석

---

## 5. Phase C. Kafka event 도입 후 설계

Kafka 도입 후에는 HTTP 요청과 비즈니스 흐름이 1:1이 아니므로 식별자를 분리한다.

### 5.1 Kafka 헤더 표준

모든 producer는 최소 다음 헤더를 넣는다.

- `traceparent`
- `tracestate` (필요 시)
- `x-correlation-id`
- `event-id`
- `causation-id`
- `source-service`
- `event-type`
- `event-version`

규칙:

- HTTP 요청에서 직접 발행된 첫 이벤트는 기존 `correlation_id`를 재사용한다.
- 각 메시지는 새 `event_id`를 가진다.
- 후속 이벤트는 직전 이벤트의 `event_id`를 `causation_id`로 사용한다.

### 5.2 Consumer 처리 규칙

Consumer는 수신한 헤더를 읽어 MDC를 복원한다.

로그 필드:

- `trace_id`
- `correlation_id`
- `event_id`
- `causation_id`
- `topic`
- `partition`
- `offset`
- `event_type`
- `outcome`

즉 운영자는 다음 두 관점으로 흐름을 복원할 수 있어야 한다.

- `trace_id`: 기술적 분산 호출 흐름
- `correlation_id`: 하나의 비즈니스 처리 흐름

### 5.3 Kafka 메트릭

Kafka 도입 후 다음 메트릭을 추가한다.

- `notio_kafka_publish_total{topic,event_type,outcome}`
- `notio_kafka_consume_total{topic,event_type,outcome}`
- `notio_kafka_consume_duration{topic,event_type}`
- `notio_kafka_consumer_lag`
- `notio_event_processing_total{event_type,outcome}`
- `notio_event_processing_duration{event_type}`

제약:

- `event_id`, `trace_id`, `correlation_id`는 메트릭 태그 금지
- 태그는 `topic`, `event_type`, `consumer_group`, `outcome`까지만 허용

---

## 6. 구현 대상 요약

현재 `backend` 기준으로 주요 반영 대상은 다음이다.

- `build.gradle.kts`
  - Actuator / Prometheus 의존성 추가
- `application.yml`, `application-local.yml`, `application-prod.yml`
  - management 및 logging 설정 보강
- `SecurityConfig`
  - actuator endpoint 허용
- 공통 요청 correlation filter 추가
- `GlobalExceptionHandler`
  - 구조화 예외 로그 통일
- `JwtAuthenticationFilter`
- `RateLimitFilter`
- `AuthService`, `OAuthAuthService`, `LocalAuthService`, `AuthAuditService`
- `ChatService`, `OllamaLlmProvider`, `PgvectorRagRetriever`
- `WebhookController`, `WebhookDispatcher`
- `ConnectionService`
- `NotificationService`
- `PushService`, `DeviceService`
- `logback-spring.xml`
  - prod JSON / local-dev text 분기

---

## 7. 테스트 계획

### 7.1 Phase A 테스트

- 공통 필터가 `/api/v1/**` 요청마다 `X-Correlation-Id`를 부여하는지 검증한다.
- 이미 전달된 `X-Correlation-Id`를 재사용하는지 검증한다.
- 요청 완료 후 MDC가 정리되는지 검증한다.
- `GlobalExceptionHandler`가 `correlation_id`가 포함된 표준 예외 로그를 남기는지 검증한다.
- `chat`, `webhook`, `auth`, `notification`, `push` 흐름에서 로그와 메트릭 호출 지점이 동작하는지 검증한다.
- `/actuator/health`, `/actuator/prometheus`가 보안 정책상 공개되는지 검증한다.
- Prometheus scrape 결과에 커스텀 메트릭이 노출되는지 검증한다.
- 민감정보가 로그에 남지 않는지 검증한다.

### 7.2 Phase B 테스트

- Gateway가 넘긴 `X-Correlation-Id`를 backend가 그대로 유지하는지 검증한다.
- tracing이 붙은 경우 `trace_id`, `span_id`, `correlation_id`가 함께 기록되는지 검증한다.
- gateway 전달 값과 fallback 생성 값이 충돌 없이 동작하는지 검증한다.

### 7.3 Phase C 테스트

- Kafka producer가 header를 정확히 넣는지 검증한다.
- Kafka consumer가 header를 읽어 MDC를 복원하는지 검증한다.
- 동일 `correlation_id`로 ingress부터 consumer까지 흐름 조회가 가능한지 검증한다.
- `event_id`, `causation_id`로 이벤트 체인을 복원할 수 있는지 검증한다.

---

## 8. 수용 기준

이 계획이 완료되면 최소 다음을 만족해야 한다.

- 운영자가 특정 API 요청을 `correlation_id` 하나로 끝까지 조회할 수 있다.
- 예외 로그도 같은 `correlation_id`를 포함한다.
- 주요 프로세스 로그가 `event`와 `outcome` 중심으로 일정한 필드 체계를 가진다.
- `/actuator/prometheus`를 통해 Prometheus가 메트릭을 수집할 수 있다.
- 현재 모놀리스 구조에 적용 가능하며, 이후 API Gateway와 Kafka event 구조로 자연스럽게 확장할 수 있다.
