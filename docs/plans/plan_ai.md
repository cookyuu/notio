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
- [ ] 기존 Spring AI 의존성 제거 또는 공존

### 테스트 / 품질
- [ ] pytest 단위 테스트
- [ ] ruff lint + mypy 타입 체크
- [ ] `ci-ai.yml` GitHub Actions 추가
