# Notio — MVP 개발 계획서

> 버전: v1.0 | 기준: MSA 전환 전 모놀리스 단계

---

## 프로젝트 구조

```
notio/
├── backend/          # Spring Boot 4.x (Java 25)
├── frontend/         # Flutter (iOS / Android)
├── ai/               # Python FastAPI — Phase 1 이후 추가
├── infra/            # Docker Compose, K8s manifests
├── docs/             # 설계서 (.docx, .md)
├── scripts/          # setup.sh, seed.sh
└── plan.md           # 이 파일 (전체 계획 인덱스)
```

---

## 세부 계획 파일

| 파일 | 범위 |
|------|------|
| [plan_backend.md](./plan_backend.md) | Spring Boot — 도메인, 레이어, 네이밍, 체크리스트 |
| [plan_frontend.md](./plan_frontend.md) | Flutter — 화면, 상태관리, 디자인 시스템, 체크리스트 |
| [plan_infra.md](./plan_infra.md) | Docker Compose, 환경변수, CI/CD 파이프라인 |
| [plan_ai.md](./plan_ai.md) | Python FastAPI RAG 서비스 (Phase 1~) |

---

## MVP 범위 (Phase 0)

### 포함
- [ ] Webhook 수신 (Claude Code hook, Slack, GitHub)
- [ ] 알림 저장 · 조회 · 읽음 처리 · 삭제
- [ ] FCM 푸시 알림 (Android 우선, iOS 추후)
- [ ] Flutter 앱 — 알림 탭 · AI 채팅 탭 · 분석 탭 · 설정 탭
- [ ] AI 채팅 — 오늘 요약, 할일 생성 (Spring AI + Ollama)
- [ ] 로컬 Docker Compose 실행 환경

### 제외 (Phase 1~)
- [ ] AI Service 분리 (Python FastAPI)
- [ ] Gmail Pub/Sub 연동
- [ ] iOS APNs (Apple Developer 계정 필요)
- [ ] Auth Service / 멀티 유저
- [ ] Kubernetes 배포

---

## 디자인 시스템 확정

| 항목 | 값 |
|------|----|
| 배경 톤 | 다크 모드 — `#0a0a12` 베이스 |
| 액센트 | 바이올렛 `#7c5cfc` / `#9b7dff` |
| 카드 스타일 | 글래스모피즘 (`rgba(255,255,255,0.08)`) |
| 텍스트 1차 | `#f0eeff` |
| 텍스트 2차 | `#a09dc0` |
| 소스 색상 | Claude `#a78bfa` · Slack `#fb923c` · GitHub `#94a3b8` · Gmail `#f87171` |

---

## 주차별 로드맵

| 주차 | 목표 | 담당 |
|------|------|------|
| 1주차 | Spring Boot 세팅 + Claude Code hook 수신 + FCM + Flutter 알림 목록 | backend + frontend |
| 2주차 | Slack · GitHub Webhook + 소스 필터 UI + Drift 로컬 캐시 | backend + frontend |
| 3주차 | Ollama + Spring AI + 채팅 탭 기본 UI + 할일 생성 API | backend + frontend |
| 4주차 | 오늘 요약 (Redis 캐시) + SSE 스트리밍 + 빠른 질문 칩 | backend + frontend |
| 5주차 | 분석 탭 · 설정 탭 · 전체 테스트 · TestFlight 준비 | frontend + QA |

---

## 네이밍 공통 규칙

- **브랜치**: `feat/`, `fix/`, `chore/`, `docs/` 접두사
- **커밋**: `feat(notification): add webhook receiver` (Conventional Commits)
- **PR 제목**: `[BE] feat: Slack webhook handler` / `[FE] feat: notification list screen`
- **환경변수**: `NOTIO_` 접두사 (예: `NOTIO_JWT_SECRET`)

---

## 참고 문서

### 종합 설계서
- [notio_blueprint.md](../blueprint/notio_blueprint.md) — Phase 0~4 전체 아키텍처, 서비스별 스펙, 데이터 모델, API 설계, 인프라, 배포 전략, 테스트 전략, 마이그레이션 플레이북, 운영 준비, 기술 의사결정 기록 등 포함

### 개발 가이드
- [CLAUDE.md](../../CLAUDE.md) — Claude Code 개발 가이드, 커밋 메시지 형식
