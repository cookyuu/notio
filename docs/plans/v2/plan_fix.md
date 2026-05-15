# LLM Timeout Exception 종합 수정 계획

## Context

`notio-backend`에서 `OllamaLlmProvider.callWithTimeout()`이 20초 타임아웃에 걸려 LLM 예외가 반복 발생하고 있다. 로그상 실제 elapsed는 22.7초로 아슬하게 초과. 단순한 타임아웃 값 문제가 아니라 코드·설정·인프라 세 레이어가 복합적으로 얽혀 있다.

---

## 근본 원인 분석 (레이어별)

### Layer 1 — 코드 버그 (Critical)

**Bug A: `@Transactional`이 LLM 호출 전체를 감싸고 있음**

`NotificationSummaryService.summarize()` (`NotificationSummaryService.java:35`)에 `@Transactional`이 붙어 있어 DB 커넥션이 LLM 응답을 기다리는 20+ 초 동안 계속 점유된다.

```
DB 트랜잭션 open → RAG 쿼리 → llmProvider.chat() [20s 대기] → updateAiSummary → 트랜잭션 close
                                 ↑ 이 구간에 DB 커넥션 낭비
```

HikariCP 기본 pool-size는 10. 알림이 동시에 10개만 들어와도 **커넥션 풀 고갈 → 연쇄 장애** 발생 가능.

**Bug B: `updateAiSummary`에 `@Transactional` 없음**

`NotificationRepository.java:88`의 `@Modifying` 쿼리에 `@Transactional`이 없어, Bug A의 외부 트랜잭션에 의존한다. Bug A를 수정하면 이 메서드가 트랜잭션 없이 실행되어 `TransactionRequiredException`이 발생한다.

### Layer 2 — 설정 문제 (High Impact)

**설정 A: `llm-timeout: 20s` 가 너무 짧음**

로그에서 elapsed가 22.7초였는데 타임아웃이 20초라 실패했다. llama3.2:3b는 cold start나 부하 시 20-30초가 걸리는 경우가 있다. 이 호출은 이미 `CompletableFuture.runAsync()`로 비동기 처리되므로 사용자 응답 시간과 무관하다. **타임아웃을 늘려도 UX에 영향 없음.**

**설정 B: `spring.ai.retry.max-attempts: 3` 복합 효과**

Spring AI의 내장 retry가 `chatModel.call()` 내부에서 동작한다. 이 retry는 `callWithTimeout()`의 Future 내부에서 실행되므로, 실패 시 retry 3회 × (응답 시간)이 합산되어 20초 타임아웃에 함께 걸린다. **retry 횟수를 줄여 타임아웃 복합 효과를 억제해야 한다.**

**설정 C: HikariCP 기본값 (pool-size 10)**

`application.yml`에 HikariCP 설정이 없어 기본값 10이 적용된다. Bug A가 수정되지 않은 상태에서는 동시 알림 10개로 pool이 고갈된다.

### Layer 3 — 인프라 문제 (Medium Impact)

**인프라 A: Ollama 컨테이너에 healthcheck 없음**

`docker-compose.yml`에서 `notio-backend`는 `ollama: condition: service_started`에만 의존한다. Ollama 프로세스가 시작됐어도 모델이 로드되기 전 요청을 받아 지연이 발생할 수 있다.

**인프라 B: Ollama 병렬 처리 설정 없음**

`OLLAMA_NUM_PARALLEL` 환경변수가 없어 기본값(1)이 적용된다. 동시 LLM 요청이 큐에 쌓이면 뒤 요청들이 앞 요청 처리 시간만큼 추가 대기한다.

**인프라 C: Backend mem_limit 768MB — JVM에 빡빡**

JVM + Spring Boot + 768MB는 GC 압박으로 응답 지연이 추가될 수 있다. llama3.2:3b 모델 자체도 ~2GB RAM을 쓰므로 Ollama와 Backend가 WSL2 공유 메모리를 경쟁한다.

---

## 구현 계획 (레이어별 우선순위)

### Layer 1 — 코드 수정 (즉시 필수)

#### 1-A. `@Transactional` 제거 (`NotificationSummaryService.java:35`)

```java
// Before
@Nullable
@Transactional
public String summarize(Notification notification) { ... }

// After
@Nullable
public String summarize(Notification notification) { ... }
```

RAG 쿼리(`ragRetriever.retrieve()`)는 내부에서 자체 read 트랜잭션을 가짐.
LLM 호출(`llmProvider.chat()`)은 트랜잭션 불필요.
DB 업데이트는 1-B에서 처리.

#### 1-B. `updateAiSummary`에 `@Transactional` 추가 (`NotificationRepository.java:88`)

```java
// Before
@Modifying
@Query("UPDATE Notification n SET n.aiSummary = :summary WHERE n.id = :id")
void updateAiSummary(@Param("id") Long id, @Param("summary") String summary);

// After
@Transactional
@Modifying
@Query("UPDATE Notification n SET n.aiSummary = :summary WHERE n.id = :id")
void updateAiSummary(@Param("id") Long id, @Param("summary") String summary);
```

### Layer 2 — 설정 조정 (`application.yml`)

#### 2-A. `llm-timeout` 20s → 45s

비동기 처리라 UX 영향 없음. llama3.2:3b cold start 수용.

```yaml
notio:
  ai:
    llm-timeout: ${NOTIO_AI_LLM_TIMEOUT:45s}
```

#### 2-B. Spring AI retry 3 → 1

타임아웃 복합 효과 차단. LLM 재시도는 Circuit Breaker가 담당.

```yaml
spring:
  ai:
    retry:
      max-attempts: ${NOTIO_LLM_RETRY_MAX_ATTEMPTS:1}
```

#### 2-C. HikariCP pool-size 명시

커넥션 풀 고갈 방어.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Layer 3 — 인프라 (`docker-compose.yml`)

#### 3-A. Ollama healthcheck 및 병렬 처리 추가

```yaml
ollama:
  image: ollama/ollama:latest
  container_name: notio-ollama
  environment:
    OLLAMA_NUM_PARALLEL: "2"
    OLLAMA_MAX_QUEUE: "4"
  healthcheck:
    test: ["CMD-SHELL", "curl -sf http://localhost:11434/api/tags || exit 1"]
    interval: 15s
    timeout: 5s
    retries: 5
    start_period: 30s
  ports:
    - "11434:11434"
  volumes:
    - ollama_data:/root/.ollama
  networks:
    - notio-network
```

#### 3-B. Backend depends_on 조건 강화

```yaml
notio-backend:
  depends_on:
    ollama:
      condition: service_healthy   # service_started → service_healthy
```

#### 3-C. Backend mem_limit 768m → 1.5g (선택)

JVM GC 압박 완화. WSL2 총 메모리 여유가 있을 때만 적용.

### Layer 4 — Resilience (권장)

#### 4-A. Resilience4j Circuit Breaker 추가

Ollama가 지속 장애 시 45초 대기 없이 즉시 fail fast.

**`build.gradle.kts`:**
```kotlin
implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
implementation("org.springframework.boot:spring-boot-starter-aop")
```

**`application.yml`:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      llmProvider:
        sliding-window-size: 5
        failure-rate-threshold: 60
        slow-call-duration-threshold: 40s
        slow-call-rate-threshold: 80
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 2
```

**`OllamaLlmProvider.java:40`에 `@CircuitBreaker` 추가:**
```java
@Override
@CircuitBreaker(name = "llmProvider", fallbackMethod = "chatFallback")
public String chat(final LlmPrompt prompt) { ... }

private String chatFallback(final LlmPrompt prompt, Throwable t) {
    log.warn("event=llm_circuit_open cause={}", t.getMessage());
    throw exceptionTranslator.llmUnavailable(
        t instanceof RuntimeException re ? re : new IllegalStateException(t));
}
```

---

## 수정 대상 파일 요약

| 우선순위 | 파일 | 변경 |
|---------|------|------|
| **Critical** | `backend/src/main/java/com/notio/notification/service/NotificationSummaryService.java:35` | `@Transactional` 제거 |
| **Critical** | `backend/src/main/java/com/notio/notification/repository/NotificationRepository.java:88` | `@Transactional` 추가 |
| **High** | `backend/src/main/resources/application.yml` | llm-timeout 45s, retry 1, HikariCP 설정 |
| **High** | `docker-compose/docker-compose.yml` | Ollama healthcheck, OLLAMA_NUM_PARALLEL |
| **Medium** | `backend/src/main/java/com/notio/ai/llm/OllamaLlmProvider.java` | Circuit Breaker 추가 |
| **Medium** | `backend/build.gradle.kts` | Resilience4j 의존성 |

---

## Phase 1 이후

아키텍처 계획상 AI 서비스가 Python FastAPI + Celery + Redis로 분리되면 이 문제는 구조적으로 해소된다. Celery worker가 LLM 요청을 완전히 분리된 프로세스에서 처리하므로 DB 커넥션과 완전히 decoupled된다.

---

## 검증 방법

1. **Code**: `NotificationSummaryServiceTest`에서 LLM 타임아웃 발생 시 `updateAiSummary`가 별도 트랜잭션으로 호출됨을 확인
2. **Integration**: Testcontainers + Mockito로 LLM을 40초 지연 Mock으로 대체, 동시 10개 알림 저장 시 DB 커넥션 수 모니터링
3. **Docker**: `docker stats`로 Ollama 컨테이너 메모리/CPU 확인, healthcheck PASS 후 백엔드 시작 여부 확인
4. **Circuit Breaker**: Ollama를 pause 후 5회 연속 실패 → circuit open → 이후 요청이 즉시 실패하는지 확인
