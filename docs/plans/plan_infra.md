# Notio — Infra 개발 계획서 (plan_infra.md)

> Docker Compose (로컬) · GitHub Actions (CI/CD) · MVP (Phase 0)

---

## 1. 디렉토리 구조

```
infra/
├── docker-compose.yml          # 로컬 전체 스택
├── docker-compose.prod.yml     # 프로덕션 오버라이드
├── .env.example                # 환경변수 템플릿
└── k8s/                        # Phase 1+ Kubernetes
    └── README.md
```

---

## 2. Docker Compose 서비스 목록

| 서비스 | 이미지 | 포트 | 의존 |
|--------|--------|------|------|
| `notio-backend` | `notio/backend:local` | 8080 | postgres, redis, ollama |
| `postgres` | `pgvector/pgvector:pg16` | 5432 | — |
| `redis` | `redis:7-alpine` | 6379 | — |
| `ollama` | `ollama/ollama:latest` | 11434 | — |

### 로컬 실행 명령

```bash
# 최초 실행
docker-compose up -d postgres redis
./scripts/setup.sh          # Ollama 모델 pull + DB 초기화

# 전체 실행
docker-compose up -d

# 백엔드만 재빌드
docker-compose up -d --build notio-backend

# 로그 확인
docker-compose logs -f notio-backend
```

---

## 3. 환경변수 (.env.example)

```dotenv
# DB
NOTIO_DB_URL=jdbc:postgresql://localhost:5432/notio
NOTIO_DB_USER=notio
NOTIO_DB_PASSWORD=notio_dev

# Redis
NOTIO_REDIS_HOST=localhost
NOTIO_REDIS_PORT=6379

# Ollama
NOTIO_OLLAMA_URL=http://localhost:11434
NOTIO_LLM_MODEL=llama3.2:3b
NOTIO_EMBED_MODEL=nomic-embed-text
NOTIO_EMBED_DIM=768
NOTIO_RAG_TOP_K=5

# JWT
NOTIO_JWT_SECRET=change-me-in-production
NOTIO_JWT_EXPIRY_MS=86400000

# Webhook 시크릿
NOTIO_SLACK_SECRET=
NOTIO_GITHUB_SECRET=
NOTIO_INTERNAL_TOKEN=notio-internal-dev

# Firebase
GOOGLE_APPLICATION_CREDENTIALS=/app/firebase-service-account.json

# Spring Profile
SPRING_PROFILES_ACTIVE=local
```

---

## 4. scripts/setup.sh

```bash
#!/bin/bash
set -e

echo ">> Ollama 모델 pull..."
docker exec notio-ollama ollama pull llama3.2:3b
docker exec notio-ollama ollama pull nomic-embed-text

echo ">> pgvector 확장 활성화..."
docker exec notio-postgres psql -U notio -d notio \
  -c "CREATE EXTENSION IF NOT EXISTS vector;"

echo ">> 완료"
```

---

## 5. GitHub Actions 파이프라인

### ci-backend.yml

```yaml
name: CI — Backend
on:
  push:
    paths: ['backend/**', 'infra/docker-compose.yml']
  pull_request:
    paths: ['backend/**']

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: pgvector/pgvector:pg16
        env:
          POSTGRES_DB: notio_test
          POSTGRES_USER: notio
          POSTGRES_PASSWORD: notio_test
        ports: ['5432:5432']
      redis:
        image: redis:7-alpine
        ports: ['6379:6379']
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: cd backend && ./gradlew test --info
      - run: cd backend && ./gradlew checkstyleMain spotbugsMain
```

### ci-frontend.yml

```yaml
name: CI — Frontend
on:
  push:
    paths: ['frontend/**']
  pull_request:
    paths: ['frontend/**']

jobs:
  analyze:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: subosito/flutter-action@v2
        with: { flutter-version: '3.x', channel: 'stable' }
      - run: cd frontend && flutter pub get
      - run: cd frontend && flutter analyze
      - run: cd frontend && flutter test
```

---

## 6. MVP 체크리스트

### Docker Compose
- [ ] `infra/docker-compose.yml` 작성
- [ ] `infra/.env.example` 작성
- [ ] `scripts/setup.sh` 작성 (Ollama 모델 pull + pgvector 확장)
- [ ] `scripts/seed.sh` 작성 (개발용 더미 알림 데이터)
- [ ] `docker-compose up -d` 후 전체 서비스 정상 기동 확인
- [ ] pgvector 확장 활성화 확인

### CI/CD
- [ ] `.github/workflows/ci-backend.yml` 작성
- [ ] `.github/workflows/ci-frontend.yml` 작성
- [ ] `backend/**` 변경 시 backend CI만 트리거 확인
- [ ] `frontend/**` 변경 시 frontend CI만 트리거 확인
- [ ] PR 머지 차단 — CI 실패 시 머지 불가 설정 (Branch Protection Rule)

### 루트 설정
- [ ] `.gitignore` — `.env`, `*.local`, `firebase-service-account.json` 제외
- [ ] `.editorconfig` — indent_style, end_of_line, charset 통일
- [ ] `README.md` — 로컬 실행 가이드 작성
