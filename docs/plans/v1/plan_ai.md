# Notio — AI Service 개발 계획서 (plan_ai.md)

> Python 3.12 · FastAPI · LangChain · Phase 1 (MVP 이후)

---

## 개요

MVP(Phase 0)에서는 Spring AI + Ollama로 AI 기능을 처리한다.
AI Service는 RAG 고도화·파인튜닝이 필요한 시점에 별도 서비스로 분리한다.

**분리 트리거 조건**
- LangChain 고급 기능 (멀티스텝 체인, Agent) 필요 시
- Hugging Face 모델 실험 필요 시
- 임베딩 파이프라인 부하가 메인 백엔드에 영향을 줄 때

---

## 1. 기술 스택 (Phase 1~)

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Python | 3.12 |
| 프레임워크 | FastAPI | 0.115.x |
| 패키지 관리 | Poetry | 1.8.x |
| RAG | LangChain | 0.3.x |
| 임베딩 | Ollama (nomic-embed-text) | — |
| 벡터 DB | pgvector (psycopg2) | — |
| 비동기 태스크 | Celery + Redis | 5.x |
| 테스트 | pytest + httpx | — |
| 린트 | ruff + mypy | — |

---

## 2. 폴더 구조 (Phase 1~)

```
ai/
├── pyproject.toml
├── Dockerfile
├── .env.example
└── app/
    ├── main.py
    ├── core/
    │   ├── config.py          # 환경변수 (pydantic-settings)
    │   ├── database.py        # pgvector 연결
    │   └── redis.py           # Redis 연결
    ├── embedding/
    │   ├── pipeline.py        # 임베딩 생성 파이프라인
    │   └── tasks.py           # Celery 비동기 태스크
    ├── retrieval/
    │   └── retriever.py       # pgvector 유사도 검색
    ├── llm/
    │   ├── provider.py        # LlmProvider (추상)
    │   ├── ollama_provider.py
    │   └── claude_provider.py # Fallback
    ├── chat/
    │   ├── service.py
    │   ├── prompt_builder.py
    │   └── router.py
    └── schemas/
        ├── embed.py
        ├── chat.py
        └── report.py
```

---

## 3. API 엔드포인트 (Phase 1~)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/embed` | 텍스트 임베딩 생성 (768d) |
| POST | `/retrieve` | top-k 유사 알림 검색 |
| POST | `/chat` | RAG 기반 응답 |
| GET | `/stream` | SSE 스트리밍 응답 |
| POST | `/summarize` | 알림 배치 요약 |
| POST | `/classify` | 우선순위 분류 |
| GET | `/report/weekly` | 주간 분석 리포트 |
| GET | `/health` | 헬스체크 |

---

## 4. Phase 1 체크리스트

### 환경 세팅
- [ ] Poetry 프로젝트 초기화
- [ ] FastAPI + uvicorn 설정
- [ ] pydantic-settings 환경변수 관리
- [ ] Docker Compose에 `ai-service` 서비스 추가
- [ ] pgvector 연결 설정
- [ ] Redis (Celery broker) 연결 설정

### 임베딩 파이프라인
- [ ] `EmbeddingPipeline` 구현 (Ollama nomic-embed-text)
- [ ] Celery 비동기 태스크로 분리
- [ ] `POST /embed` 엔드포인트
- [ ] 실패 시 재시도 (max_retries=3)

### RAG 검색
- [ ] pgvector 코사인 유사도 검색 구현
- [ ] `POST /retrieve` 엔드포인트
- [ ] `PromptBuilder` — 컨텍스트 + 질의 조합

### LLM 연동
- [ ] `LlmProvider` 추상 인터페이스 정의
- [ ] `OllamaProvider` 구현
- [ ] `ClaudeProvider` Fallback 구현
- [ ] `POST /chat` 단건 응답
- [ ] `GET /stream` SSE 스트리밍

### Spring Boot 연동
- [ ] Spring Boot `ChatService` → AI Service REST 호출로 교체
- [ ] OpenFeign 클라이언트 추가
- [ ] Feature Flag 기반 점진적 전환 (`notio.ai-service.enabled`)
- [ ] Fallback 구현 (AI Service 장애 시 Spring AI 사용)
- [ ] Canary 배포 (10% → 100%)
- [ ] 기존 Spring AI 의존성 제거 또는 공존

### 테스트 / 품질
- [ ] pytest 단위 테스트
- [ ] ruff lint + mypy 타입 체크
- [ ] `ci-ai.yml` GitHub Actions 추가

---

## 5. LLM Provider 전략 패턴

AI Service는 여러 LLM을 지원하도록 **전략 패턴**을 사용합니다.

### Provider 인터페이스

```python
from abc import ABC, abstractmethod
from typing import List, Generator

class LlmProvider(ABC):
    @abstractmethod
    def chat(self, messages: List[Message]) -> str:
        pass

    @abstractmethod
    def stream(self, messages: List[Message]) -> Generator[str, None, None]:
        pass
```

### 구현체

**OllamaProvider:**
```python
class OllamaProvider(LlmProvider):
    def chat(self, messages):
        # Ollama API 호출
        response = ollama.chat(model="llama3.2:3b", messages=messages)
        return response["message"]["content"]

    def stream(self, messages):
        for chunk in ollama.chat(model="llama3.2:3b", messages=messages, stream=True):
            yield chunk["message"]["content"]
```

**ClaudeProvider (Fallback):**
```python
class ClaudeProvider(LlmProvider):
    def chat(self, messages):
        # Claude API 호출
        response = anthropic_client.messages.create(
            model="claude-sonnet-4-5-20250929",
            messages=messages
        )
        return response.content[0].text
```

### 환경변수 기반 선택

```python
# config.py
provider_type = os.getenv("AI_MODEL_PROVIDER", "ollama")  # ollama | claude | openai
fallback_enabled = os.getenv("AI_FALLBACK_ENABLED", "true").lower() == "true"

def get_provider() -> LlmProvider:
    if provider_type == "ollama":
        return OllamaProvider()
    elif provider_type == "claude":
        return ClaudeProvider()
    else:
        raise ValueError(f"Unknown provider: {provider_type}")
```

---

## 6. 마이그레이션 절차 (Phase 0 → Phase 1)

### Step 1: AI Service 배포 (병렬 운영)
- AI Service를 별도 컨테이너로 배포 (:8090)
- Spring Boot는 아직 변경하지 않음
- Smoke Test로 AI Service 단독 동작 확인

### Step 2: Spring Boot OpenFeign 클라이언트 추가

```java
@FeignClient(name = "ai-service", url = "${notio.ai-service.url}")
public interface AiServiceClient {
    @PostMapping("/chat")
    ChatResponse chat(@RequestBody ChatRequest request);

    @GetMapping("/stream")
    Flux<String> stream(@RequestParam String message);
}
```

### Step 3: ChatService 로직 교체

```java
@Service
public class ChatService {
    private final AiServiceClient aiServiceClient;
    private final SpringAiChatModel springAiChatModel;  // Fallback 유지

    @Value("${notio.ai-service.enabled:false}")
    private boolean aiServiceEnabled;

    public ChatResponse chat(ChatRequest request) {
        if (aiServiceEnabled) {
            try {
                return aiServiceClient.chat(request);
            } catch (Exception e) {
                log.warn("AI Service unavailable, fallback to Spring AI", e);
                return springAiChatModel.call(request);  // Fallback
            }
        }
        return springAiChatModel.call(request);
    }
}
```

### Step 4: Feature Flag 기반 점진적 전환
- `notio.ai-service.enabled=false` → 기존 Spring AI 사용
- Canary: 10% 트래픽 → AI Service
- 모니터링 (Prometheus + Grafana):
  - 응답 시간
  - 에러율
  - 임베딩 품질 (코사인 유사도)
- 문제 없으면 100% 전환

### Step 5: Spring AI 의존성 제거
- Feature Flag `true`로 고정
- Spring AI 관련 코드 제거
- Gradle 의존성 제거

### 롤백 계획
- Feature Flag `false`로 즉시 전환
- AI Service 장애 시 자동 Fallback
