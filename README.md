# Notio

> 개발자를 위한 AI 기반 통합 알림 허브

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Flutter](https://img.shields.io/badge/Flutter-3.27.3-02569B?logo=flutter)](https://flutter.dev)
[![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk)](https://openjdk.org)
[![Dart](https://img.shields.io/badge/Dart-3.6.1-0175C2?logo=dart)](https://dart.dev)

**Backend Phase 0 완료** ✅ | **Frontend Phase 0 완료** ✅ | Phase 1 개발 중 🚧

## 개요

**Notio**는 Claude Code, Slack, GitHub, Gmail 등 다양한 개발 도구의 알림을 한 곳에서 관리하고, AI를 활용하여 자동으로 요약하고 우선순위를 분류하는 통합 알림 플랫폼입니다.

### 핵심 기능

- 다중 소스 통합: 여러 개발 도구 알림을 한 곳에서 관리
- AI 자동화: LLM 기반 알림 요약, 우선순위 분류, 할일 자동 생성
- 패턴 분석: 알림 패턴 분석으로 생산성 인사이트 제공
- 실시간 푸시: FCM/APNs를 통한 즉시 알림
- 세련된 UI: 다크 모드 + 바이올렛 액센트의 글래스모피즘 디자인

## 기술 스택

### Backend
- **언어**: Java 25
- **프레임워크**: Spring Boot 4.x
- **빌드**: Gradle 8.x (Kotlin DSL)
- **데이터베이스**: PostgreSQL 16 + pgvector
- **캐시**: Redis 7
- **AI**: Spring AI + Ollama (llama3.2:3b)

### Frontend
- **언어**: Dart 3.x
- **프레임워크**: Flutter 3.x
- **상태관리**: Riverpod + Flutter Hooks
- **네트워크**: Dio + Retrofit
- **로컬 DB**: Drift (SQLite)

### Infrastructure
- **로컬**: Docker Compose
- **프로덕션**: Kubernetes (계획)

## 프로젝트 구조

```
notio/
├── backend/                 # Spring Boot 백엔드
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/notio/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   └── test/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── frontend/                # Flutter 프론트엔드
│   ├── lib/
│   │   ├── core/           # 코어 유틸리티
│   │   ├── features/       # 기능별 모듈
│   │   └── shared/         # 공유 컴포넌트
│   └── pubspec.yaml
│
├── docs/                    # 문서
│   └── blueprint/
│       └── notio_blueprint.md
│
├── infra/                   # 인프라 설정
│   └── init-scripts/
│
├── scripts/                 # 유틸리티 스크립트
│   └── setup.sh
│
├── docker-compose.yml
├── .env.example
└── README.md
```

## 시작하기

### 사전 요구사항

- Java 25
- Flutter 3.x
- Docker & Docker Compose
- (선택) NVIDIA GPU (Ollama 가속)

### 설치 및 실행

1. **저장소 클론**
```bash
git clone https://github.com/yourusername/notio.git
cd notio
```

2. **환경 설정**
```bash
# 자동 설정 스크립트 실행 (권장)
./scripts/setup.sh

# 또는 수동 설정
cp .env.example .env
# .env 파일 수정
```

3. **Docker 서비스 시작**
```bash
docker-compose up -d
```

4. **Backend 실행**
```bash
cd backend
./gradlew bootRun
```

5. **Frontend 실행**
```bash
cd frontend
flutter pub get
flutter run
```

### API 문서

Backend 실행 후 다음 URL에서 Swagger UI를 확인할 수 있습니다:
```
http://localhost:8080/swagger-ui.html
```

## 개발 가이드

상세한 개발 가이드는 [`.claude/claude.md`](.claude/claude.md)를 참조하세요.

### 주요 개발 규칙

- **네이밍**: Backend는 PascalCase/camelCase, Frontend는 snake_case
- **레이어 분리**: Controller → Service → Repository 계층 엄격히 준수
- **도메인 우선**: 패키지는 도메인별로 수평 분리
- **테스트 필수**: 모든 Service 계층 메서드는 단위 테스트 작성

### 브랜치 전략

```
main                    # 프로덕션 브랜치
└── feat/{feature}      # 새 기능
└── fix/{bug}           # 버그 수정
└── chore/{task}        # 빌드, 설정
└── docs/{doc}          # 문서 작성
```

### 커밋 메시지

```
<type>: <subject>

<body>
```

**Type**: `feature`, `fix`, `refactor`, `docs`, `test`, `chore`, `style`

## 📅 로드맵 및 진행 상황

### Frontend 개발 (5주 MVP)

| Phase | 기간 | 목표 | 상태 |
|-------|------|------|------|
| **Phase 0** | 1주차 | 환경 세팅 및 기본 인프라 | ✅ 완료 |
| **Phase 1** | 2주차 | 인증 및 알림 탭 | 🚧 진행 중 |
| **Phase 2** | 3주차 | AI 채팅 탭 | 📋 예정 |
| **Phase 3** | 4주차 | 분석 및 설정 탭 | 📋 예정 |
| **Phase 4** | 5주차 | 푸시 알림 및 통합 | 📋 예정 |

### Backend 개발 (5주 MVP)

| Phase | 기간 | 목표 | 상태 |
|-------|------|------|------|
| **Phase 0** | 1주차 | 환경 세팅 및 기본 인프라 | ✅ 완료 |
| **Phase 1** | 2주차 | 인증 및 알림 API | 📋 예정 |
| **Phase 2** | 3주차 | AI 채팅 API | 📋 예정 |
| **Phase 3** | 4주차 | 분석 및 설정 API | 📋 예정 |
| **Phase 4** | 5주차 | 푸시 알림 및 통합 테스트 | 📋 예정 |

### 장기 아키텍처 진화

| Phase | 기간 | 목표 |
|-------|------|------|
| **Phase 5** | 3-6개월 | AI Service 분리 (Python/FastAPI) |
| **Phase 6** | 6-9개월 | Notification/Webhook Service 분리 |
| **Phase 7** | 9-12개월 | Chat/Todo Service 분리 |
| **Phase 8** | 12개월+ | MSA 완성 (Auth, Analytics, Gateway) |

## ✅ Phase 0 완료 현황

### Frontend ✅
- Flutter 프로젝트 초기화
- Feature-first 폴더 구조
- 디자인 시스템 (AppColors, AppTextStyles, AppSpacing)
- 공통 위젯 (GlassCard, SourceBadge)
- 라우팅 설정 (go_router)
- 로컬 DB (Drift)
- 네트워크 클라이언트 (Dio + Interceptors)
- **12개 테스트 통과**

상세: [`frontend/PHASE0_COMPLETED.md`](frontend/PHASE0_COMPLETED.md)

### Backend ✅
- Spring Boot 4.0.0 프로젝트 초기화
- PostgreSQL 16 + pgvector 연동
- Redis 캐시 설정
- Docker Compose 환경 구성
- 기본 도메인 모델 설계

상세: [`backend/PHASE0_COMPLETED.md`](backend/PHASE0_COMPLETED.md)

## 라이선스

MIT License

## 문의

이슈나 질문이 있으시면 [GitHub Issues](https://github.com/yourusername/notio/issues)에 등록해주세요.
