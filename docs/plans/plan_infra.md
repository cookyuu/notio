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

### Phase 0 (MVP) — 모놀리스

| 서비스 | 이미지 | 포트 | 의존 |
|--------|--------|------|------|
| `notio-backend` | `notio/backend:local` | 8080 | postgres, redis, ollama |
| `postgres` | `pgvector/pgvector:pg16` | 5432 | — |
| `redis` | `redis:7-alpine` | 6379 | — |
| `ollama` | `ollama/ollama:latest` | 11434 | — |

### Phase 2+ — MSA 분리 (Database per Service)

Phase 2 이후 각 서비스는 독립 DB를 가집니다:

| 서비스 | 이미지 | 포트 | 비고 |
|--------|--------|------|------|
| `notification-db` | `pgvector/pgvector:pg16` | 5433 | pgvector 확장 필수 |
| `webhook-db` | `postgres:16` | 5434 | — |
| `chat-db` | `postgres:16` | 5435 | — |
| `todo-db` | `postgres:16` | 5436 | — |
| `push-db` | `postgres:16` | 5437 | — |
| `analytics-db` | `postgres:16` | 5438 | — |
| `auth-db` | `postgres:16` | 5439 | Phase 4 |
| `kafka` | `confluentinc/cp-kafka:7.6` | 9092 | Phase 2+ |
| `zookeeper` | `confluentinc/cp-zookeeper:7.6` | 2181 | Kafka 의존 |

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
        with: { java-version: '25', distribution: 'temurin' }
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

---

## 7. Kubernetes 배포 (Phase 1+)

### 서비스별 리소스

| 서비스 | Replicas | CPU Req/Limit | Memory Req/Limit | HPA 조건 |
|--------|----------|---------------|------------------|----------|
| api-gateway | 2 | 200m / 500m | 256Mi / 512Mi | CPU > 70% |
| notification-service | 2 | 200m / 500m | 256Mi / 512Mi | CPU > 70% |
| webhook-service | 2 | 100m / 300m | 128Mi / 256Mi | — |
| chat-service | 2 | 200m / 500m | 256Mi / 512Mi | 연결수 > 100 |
| todo-service | 1 | 100m / 300m | 128Mi / 256Mi | — |
| analytics-service | 1 | 100m / 500m | 256Mi / 512Mi | — |
| auth-service | 2 | 200m / 500m | 256Mi / 512Mi | CPU > 70% |
| push-service | 2 | 100m / 300m | 128Mi / 256Mi | Kafka Lag > 1000 |
| ai-service | 1 | 500m / 2000m | 1Gi / 4Gi | — (Ollama RAM 제약) |
| kafka | 3 | 500m / 1000m | 1Gi / 2Gi | — (StatefulSet) |

### Deployment 예시 (notification-service)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notification-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: notification-service
  template:
    metadata:
      labels:
        app: notification-service
    spec:
      containers:
      - name: notification-service
        image: ghcr.io/notio/notification:latest
        ports:
        - containerPort: 8081
        env:
        - name: NOTIFICATION_DB_URL
          valueFrom:
            secretKeyRef:
              name: db-secrets
              key: notification-db-url
        resources:
          requests:
            cpu: 200m
            memory: 256Mi
          limits:
            cpu: 500m
            memory: 512Mi
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 20
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: notification-service
spec:
  selector:
    app: notification-service
  ports:
  - port: 8081
    targetPort: 8081
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: notification-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: notification-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## 8. 배포 전략

### 개발 환경 (dev)
- **Rolling Update**
- main 머지 시 자동 배포
- 다운타임 없음
- 롤백: `kubectl rollout undo deployment/notification-service`

### 프로덕션 (prod)
- **Blue-Green**
- 구버전(Blue) 유지 중 신버전(Green) 배포
- 검증 후 Load Balancer 전환
- 롤백: LB를 Blue로 즉시 전환

### AI Service
- **Canary (10% → 100%)**
- 신 모델을 10% 트래픽으로 검증
- 문제 없으면 100% 전환
- 롤백: 10% → 0% 즉시

---

## 9. 관측성 (Observability)

### 메트릭 (Prometheus + Grafana)
- Spring Actuator `/metrics` 노출
- 수집 항목:
  - CPU · 메모리 · 디스크 사용률
  - HTTP 요청 수 · 레이턴시 (P50, P95, P99)
  - DB 커넥션 풀 상태
  - Kafka Lag

### 로그 (Loki + Promtail)
- Logback JSON 포맷
```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "level": "INFO",
  "logger": "com.notio.notification.NotificationService",
  "message": "Notification saved",
  "traceId": "abc123",
  "spanId": "def456",
  "service": "notification-service"
}
```

### 분산 트레이싱 (Zipkin)
- HTTP `traceparent` 헤더 자동 전파
- Trace 구간:
  - Flutter → API Gateway
  - Gateway → 각 서비스
  - Chat → AI Service
  - 서비스 → Kafka

### 알림 (Alertmanager → Slack)
- CPU > 80% (5분 지속)
- 메모리 > 90%
- HTTP 5xx 에러율 > 5%
- Kafka Lag > 1000
