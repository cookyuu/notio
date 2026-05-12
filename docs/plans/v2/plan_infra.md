# Plan: 인프라 및 배포

> **대상 버전**: v2.1
> **작성일**: 2026-05-12
> **연관 Blueprint**: `docs/blueprint/notio_blueprint_v2.md` §10, §12, §13

---

## 개요

v2.1 인프라는 v1.0의 단순 Docker Compose에서 다음을 추가한다:
- **Nginx** — 리버스 프록시 + Flutter Web 정적 파일 서빙 + SSE 지원
- **Certbot** — Let's Encrypt TLS 자동 발급/갱신
- **Ollama** — 선택적 기동 (profiles), 요약 파이프라인 전용

---

## Phase 1: VPS 프로비저닝

### 1-1. VPS 선택

| 시나리오 | 권장 사양 | 공급사 | 월 비용 |
|---------|---------|------|--------|
| Ollama 포함 | 4vCPU / 8GB RAM / 80GB SSD | Hetzner CX32 | ~€9.9 |
| Ollama 미사용 (Claude API) | 2vCPU / 4GB RAM / 40GB SSD | Hetzner CX22 | ~€3.9 |

**대안**: Oracle Cloud Free Tier (4GB RAM 영구 무료), DigitalOcean, Vultr

### 1-2. 초기 서버 설정

```bash
# 1. 비루트 사용자 생성
adduser notio
usermod -aG sudo notio

# 2. SSH 키 설정
mkdir -p /home/notio/.ssh
cp ~/.ssh/authorized_keys /home/notio/.ssh/
chown -R notio:notio /home/notio/.ssh
chmod 700 /home/notio/.ssh

# 3. UFW 방화벽
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable

# 4. Docker 설치
curl -fsSL https://get.docker.com | sh
usermod -aG docker notio

# 5. Docker Compose v2 확인
docker compose version
```

### 1-3. 디렉토리 구조

```
/home/notio/
├── notio/                      # 저장소 클론 위치
│   ├── docker-compose/
│   │   ├── docker-compose.prod.yml
│   │   └── docker-compose.dev.yml
│   ├── nginx/
│   │   ├── conf.d/
│   │   │   └── notio.conf
│   │   └── certbot/
│   │       └── www/
│   ├── backend/
│   └── frontend/
└── .env                        # 환경변수 (저장소 밖에 위치)
```

---

## Phase 2: Docker Compose 구성

### 2-1. `docker-compose.prod.yml`

```yaml
services:

  nginx:
    image: nginx:1.25-alpine
    container_name: notio-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - notio_web_dist:/var/www/notio-web:ro
      - certbot_webroot:/var/www/certbot:ro
      - certbot_certs:/etc/letsencrypt:ro
    depends_on:
      - notio-backend
    restart: unless-stopped
    networks:
      - notio-network

  certbot:
    image: certbot/certbot:latest
    container_name: notio-certbot
    volumes:
      - certbot_webroot:/var/www/certbot
      - certbot_certs:/etc/letsencrypt
    entrypoint: >
      /bin/sh -c "trap exit TERM;
      while :; do
        certbot renew --webroot -w /var/www/certbot --quiet;
        sleep 12h;
        wait $!;
      done;"
    restart: unless-stopped

  notio-backend:
    image: notio/backend:latest
    container_name: notio-backend
    expose:
      - "8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - NOTIO_DB_URL=jdbc:postgresql://notio-postgres:5432/notio
      - NOTIO_DB_USERNAME=${NOTIO_DB_USERNAME}
      - NOTIO_DB_PASSWORD=${NOTIO_DB_PASSWORD}
      - NOTIO_REDIS_HOST=notio-redis
      - NOTIO_JWT_SECRET=${NOTIO_JWT_SECRET}
      - NOTIO_WEBHOOK_SECRET_GITHUB=${NOTIO_WEBHOOK_SECRET_GITHUB}
      - NOTIO_CREDENTIAL_ENCRYPTION_KEY=${NOTIO_CREDENTIAL_ENCRYPTION_KEY}
      - NOTIO_AI_PROVIDER=${NOTIO_AI_PROVIDER:-ollama}
      - NOTIO_AI_SUMMARIZE_SOURCES=${NOTIO_AI_SUMMARIZE_SOURCES:-CLAUDE,CODEX}
      - NOTIO_AI_LLM_TIMEOUT=${NOTIO_AI_LLM_TIMEOUT:-20s}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY:-}
      - SPRING_AI_OLLAMA_BASE_URL=http://notio-ollama:11434
    depends_on:
      notio-postgres:
        condition: service_healthy
      notio-redis:
        condition: service_healthy
    restart: unless-stopped
    networks:
      - notio-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  notio-postgres:
    image: ankane/pgvector:v0.5.1
    container_name: notio-postgres
    expose:
      - "5432"
    environment:
      POSTGRES_DB: notio
      POSTGRES_USER: ${NOTIO_DB_USERNAME}
      POSTGRES_PASSWORD: ${NOTIO_DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped
    networks:
      - notio-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${NOTIO_DB_USERNAME} -d notio"]
      interval: 10s
      timeout: 5s
      retries: 5

  notio-redis:
    image: redis:7-alpine
    container_name: notio-redis
    expose:
      - "6379"
    command: redis-server --requirepass ${NOTIO_REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    restart: unless-stopped
    networks:
      - notio-network
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${NOTIO_REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  notio-ollama:
    image: ollama/ollama:latest
    container_name: notio-ollama
    expose:
      - "11434"
    volumes:
      - ollama_data:/root/.ollama
    restart: unless-stopped
    networks:
      - notio-network
    profiles:
      - with-ollama

volumes:
  postgres_data:
  redis_data:
  ollama_data:
  notio_web_dist:
  certbot_webroot:
  certbot_certs:

networks:
  notio-network:
    driver: bridge
```

### 2-2. `docker-compose.dev.yml` (로컬 개발)

```yaml
services:
  notio-postgres:
    image: ankane/pgvector:v0.5.1
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: notio
      POSTGRES_USER: notio
      POSTGRES_PASSWORD: notio_dev
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data

  notio-redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  notio-ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_dev_data:/root/.ollama
    profiles:
      - with-ollama

volumes:
  postgres_dev_data:
  ollama_dev_data:
```

---

## Phase 3: Nginx 설정

### 3-1. `nginx/conf.d/notio.conf`

```nginx
# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name your-domain.com;

    # Certbot ACME challenge
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS 메인 서버
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers off;

    # HSTS
    add_header Strict-Transport-Security "max-age=63072000" always;

    # Flutter Web (SPA — GoRouter deep link 지원)
    root /var/www/notio-web;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API (SSE 필수 설정 포함)
    location /api/ {
        proxy_pass         http://notio-backend:8080;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Connection '';
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_buffering    off;        # SSE: 버퍼링 비활성화
        proxy_cache        off;        # SSE: 캐시 비활성화
        proxy_read_timeout 3600s;      # SSE: 연결 유지 (1시간)
    }

    # Actuator 외부 차단
    location /actuator/ {
        allow 127.0.0.1;
        deny  all;
        proxy_pass http://notio-backend:8080;
    }

    # 정적 파일 캐싱
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

**SSE 주의사항:**
- `proxy_buffering off` — SSE 이벤트가 즉시 클라이언트에 전달되도록 필수
- `proxy_read_timeout 3600s` — SSE 연결이 1시간 동안 유지되도록 설정
- `proxy_cache off` — 캐시가 SSE 스트림을 방해하지 않도록

---

## Phase 4: TLS 인증서 발급

### 4-1. 최초 발급

```bash
# 저장소 클론 후
cd /home/notio/notio

# Nginx만 먼저 기동 (HTTP만으로 ACME challenge 처리)
docker compose -f docker-compose/docker-compose.prod.yml up -d nginx

# 인증서 발급
docker compose -f docker-compose/docker-compose.prod.yml run --rm certbot \
  certbot certonly \
  --webroot \
  -w /var/www/certbot \
  -d your-domain.com \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email

# Nginx 재시작 (HTTPS 설정 활성화)
docker compose -f docker-compose/docker-compose.prod.yml restart nginx
```

### 4-2. 자동 갱신 확인

```bash
# 갱신 테스트 (실제 갱신 없음)
docker compose -f docker-compose/docker-compose.prod.yml run --rm certbot \
  certbot renew --dry-run

# Certbot 컨테이너가 12시간마다 자동 갱신 실행 (docker-compose.prod.yml 참조)
```

---

## Phase 5: 배포 절차

### 5-1. 최초 배포

```bash
# 1. 저장소 클론
git clone https://github.com/your-org/notio.git /home/notio/notio
cd /home/notio/notio

# 2. 환경변수 파일 작성
cp .env.example /home/notio/.env
# /home/notio/.env 편집 (아래 환경변수 섹션 참조)

# 3. 환경변수 심볼릭 링크
ln -s /home/notio/.env .env

# 4. Flutter Web 빌드
cd frontend
flutter build web --release --web-renderer html \
  --dart-define=API_BASE_URL=https://your-domain.com
cd ..

# 5. 빌드 결과물을 Docker 볼륨에 복사
docker volume create notio_notio_web_dist
docker run --rm \
  -v notio_notio_web_dist:/target \
  -v $(pwd)/frontend/build/web:/source:ro \
  alpine sh -c "cp -r /source/. /target/"

# 6. Backend 이미지 빌드
docker build -t notio/backend:latest ./backend

# 7. TLS 인증서 발급 (Phase 4 참조)

# 8. 전체 서비스 기동
# Ollama 포함:
docker compose -f docker-compose/docker-compose.prod.yml \
  --profile with-ollama up -d

# Ollama 미포함 (Claude API 사용):
docker compose -f docker-compose/docker-compose.prod.yml up -d

# 9. Ollama 모델 다운로드 (Ollama 사용 시)
docker exec notio-ollama ollama pull llama3.2:3b
docker exec notio-ollama ollama pull nomic-embed-text

# 10. 웹훅 URL 등록
# Claude Code: settings.json → webhook_url: https://your-domain.com/api/v1/webhook/claude
# GitHub: Settings → Webhooks → https://your-domain.com/api/v1/webhook/github
# Slack: Event Subscriptions → https://your-domain.com/api/v1/webhook/slack
```

### 5-2. 업데이트 배포

```bash
cd /home/notio/notio
git pull origin main

# Frontend 변경 시
cd frontend
flutter build web --release --web-renderer html \
  --dart-define=API_BASE_URL=https://your-domain.com
docker run --rm \
  -v notio_notio_web_dist:/target \
  -v $(pwd)/build/web:/source:ro \
  alpine sh -c "rm -rf /target/* && cp -r /source/. /target/"
docker compose -f docker-compose/docker-compose.prod.yml restart nginx
cd ..

# Backend 변경 시
docker build -t notio/backend:latest ./backend
docker compose -f docker-compose/docker-compose.prod.yml \
  up -d --no-deps notio-backend
```

---

## Phase 6: 환경변수 관리

### 6-1. `.env.example`

```dotenv
# ========== Database ==========
NOTIO_DB_USERNAME=notio
NOTIO_DB_PASSWORD=CHANGE_ME_STRONG_PASSWORD

# ========== Redis ==========
NOTIO_REDIS_PASSWORD=CHANGE_ME_REDIS_PASSWORD

# ========== JWT ==========
NOTIO_JWT_SECRET=CHANGE_ME_AT_LEAST_256BIT_RANDOM_STRING
NOTIO_JWT_EXPIRATION_MS=86400000

# ========== Webhook Secrets ==========
NOTIO_WEBHOOK_SECRET_GITHUB=CHANGE_ME_GITHUB_WEBHOOK_SECRET
NOTIO_WEBHOOK_SECRET_SLACK=CHANGE_ME_SLACK_SIGNING_SECRET

# ========== Credential Encryption ==========
# AES-256-GCM 키 (32바이트 Base64 인코딩)
NOTIO_CREDENTIAL_ENCRYPTION_KEY=CHANGE_ME_32BYTE_BASE64_KEY

# ========== AI / LLM ==========
NOTIO_AI_PROVIDER=ollama          # ollama | anthropic | openai
NOTIO_AI_SUMMARIZE_SOURCES=CLAUDE,CODEX   # 빈 값 = 전체 소스
NOTIO_AI_LLM_TIMEOUT=20s

# Anthropic (provider=anthropic 시)
ANTHROPIC_API_KEY=
```

### 6-2. 보안 키 생성

```bash
# NOTIO_JWT_SECRET (256비트)
openssl rand -base64 32

# NOTIO_CREDENTIAL_ENCRYPTION_KEY (AES-256 = 32바이트)
openssl rand -base64 32

# NOTIO_WEBHOOK_SECRET_GITHUB (랜덤)
openssl rand -hex 32
```

---

## Phase 7: 운영 및 모니터링

### 7-1. `application-prod.yml` (Actuator 보호)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
  endpoint:
    health:
      show-details: never

spring:
  jpa:
    show-sql: false
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2

logging:
  level:
    root: WARN
    com.notio: INFO
  pattern:
    console: >
      %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36}
      trace_id=%X{traceId} span_id=%X{spanId} - %msg%n
```

### 7-2. Prometheus + Grafana (기존 스택 활용)

기존 Prometheus 설정에 새 메트릭 추가 scrape:

```yaml
# prometheus.yml 추가 없음 (notio-backend actuator/prometheus 이미 등록 가정)
```

**Grafana 패널 추가 권장:**

| 패널 | PromQL |
|------|--------|
| AI 요약 성공률 | `rate(notio_ai_summarization_total{outcome="success"}[5m]) / rate(notio_ai_summarization_total[5m])` |
| AI 요약 지연 p95 | `histogram_quantile(0.95, rate(notio_ai_summarization_duration_seconds_bucket[5m]))` |
| Digest 전달 건수 | `sum(notio_digest_delivery_total) by (channel_type)` |
| 채널 전달 성공률 | `rate(notio_channel_delivery_total{outcome="success"}[5m])` by channel_type |
| DEAD 로그 누적 | `notio_channel_delivery_total{outcome="dead"}` |

**알림 규칙 (권장):**
```yaml
# Grafana Alert
- name: AI 요약 실패 급증
  condition: notio_ai_summarization_total{outcome="failure"} > 10 (5분)
  severity: warning

- name: 채널 전달 DEAD 증가
  condition: rate(notio_channel_delivery_total{outcome="dead"}[15m]) > 0
  severity: critical
```

### 7-3. 로그 확인

```bash
# 전체 로그 스트리밍
docker compose -f docker-compose/docker-compose.prod.yml logs -f notio-backend

# 특정 이벤트만 필터
docker compose -f docker-compose/docker-compose.prod.yml logs notio-backend \
  | grep "event=ai_summarization_failed"

# Digest 처리 로그
docker compose -f docker-compose/docker-compose.prod.yml logs notio-backend \
  | grep "event=digest_"
```

### 7-4. 백업 전략

**cron 설정 (VPS에서 직접):**
```bash
crontab -e
# 매일 새벽 3시 백업
0 3 * * * /home/notio/notio/scripts/backup.sh >> /home/notio/logs/backup.log 2>&1
```

**`scripts/backup.sh`:**
```bash
#!/bin/bash
set -e
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=/home/notio/backups

mkdir -p $BACKUP_DIR

# PostgreSQL 백업
docker exec notio-postgres pg_dump \
  -U $NOTIO_DB_USERNAME \
  -d notio \
  --format=custom \
  -f /tmp/notio_${DATE}.dump

docker cp notio-postgres:/tmp/notio_${DATE}.dump $BACKUP_DIR/
docker exec notio-postgres rm /tmp/notio_${DATE}.dump

# 30일 이상 된 백업 삭제
find $BACKUP_DIR -name "notio_*.dump" -mtime +30 -delete

echo "[$(date)] Backup completed: notio_${DATE}.dump"
```

**복구 절차:**
```bash
# 백업에서 복구
docker exec -i notio-postgres pg_restore \
  -U $NOTIO_DB_USERNAME \
  -d notio \
  --clean \
  < /home/notio/backups/notio_YYYYMMDD_HHMMSS.dump
```

---

## Phase 8: 비용 총계

| 항목 | Ollama 사용 | Claude API 사용 |
|------|------------|----------------|
| Hetzner VPS | €9.9/월 (CX32) | €3.9/월 (CX22) |
| 도메인 (.com) | ~€12/년 ≈ €1/월 | €1/월 |
| LLM API | €0 | $3-5/월 |
| Let's Encrypt | 무료 | 무료 |
| **합계** | **~€11/월** | **~€9-11/월** |

---

## 배포 전 체크리스트

```
[ ] VPS 방화벽: 22/80/443만 허용
[ ] .env 파일: 저장소 밖 위치 (/home/notio/.env)
[ ] NOTIO_CREDENTIAL_ENCRYPTION_KEY: 32바이트 랜덤 키
[ ] NOTIO_JWT_SECRET: 최소 256비트
[ ] TLS 인증서: certbot certonly 성공 확인
[ ] Nginx HTTPS 리다이렉트 동작 확인 (curl -I http://your-domain.com)
[ ] Backend healthcheck: curl https://your-domain.com/api/v1/actuator/health
  → 주의: Nginx에서 /actuator/ 외부 차단 → 127.0.0.1만 허용
  → 따라서 외부에서는 /api/actuator/health 로 확인 (Nginx /api/ 경로로 프록시)
[ ] Flyway 마이그레이션 확인: V12, V13, V14 모두 성공
[ ] Ollama 모델 다운로드 완료 (Ollama 사용 시)
  → docker exec notio-ollama ollama list
[ ] 웹훅 URL 등록 (GitHub, Slack, Claude Code)
[ ] E2E 테스트: 웹훅 수신 → AI 요약 → 채널 전달 확인
[ ] Delivery Feed 화면: 전달된 메시지 버블 확인
```
