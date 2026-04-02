# Claude Code 개발 가이드

## 프로젝트 개요

**Notio**는 개발자를 위한 AI 기반 통합 알림 허브입니다.

### 핵심 기능
- 🔔 **다중 소스 통합**: Claude Code, Slack, GitHub, Gmail 등 개발 도구 알림을 한 곳에서 관리
- 🤖 **AI 자동화**: LLM 기반 알림 요약, 우선순위 분류, 할일 자동 생성
- 📊 **패턴 분석**: 알림 패턴 분석으로 생산성 인사이트 제공
- 📱 **실시간 푸시**: FCM/APNs를 통한 즉시 알림
- 🌙 **세련된 UI**: 다크 모드 + 바이올렛 액센트의 글래스모피즘 디자인

### 기술 스택
- **Frontend**: Flutter 3.x (Riverpod, go_router, Drift)
- **Backend**: Spring Boot 3.3 + Java 21 (모놀리스 → MSA)
- **AI**: Python 3.12 + FastAPI (LangChain, Ollama)
- **Database**: PostgreSQL 16 + pgvector, Redis 7
- **Message**: Apache Kafka 3.6
- **Infra**: Docker Compose (로컬), Kubernetes (프로덕션)

### 아키텍처 진화
| Phase | 기간 | 목표 |
|-------|------|------|
| **Phase 0** | 5주 (MVP) | Spring Boot 모놀리스 완성 |
| **Phase 1** | 3-6개월 | AI Service 분리 (Python) |
| **Phase 2** | 6-9개월 | Notification/Webhook 분리 |
| **Phase 3** | 9-12개월 | Chat/Todo 분리 |
| **Phase 4** | 12개월+ | MSA 완성 (Auth, Analytics, Gateway) |

### 설계 문서
상세 설계는 `/docs/blueprint/notio_blueprint.md` 참조

## 커밋 메시지

### 형식

```
<type>: <subject>
<body>
```

### Type

- `feature`: 새로운 기능 추가
- `fix`: 버그 수정
- `refactor`: 리팩토링 (기능 변경 없음)
- `docs`: 문서 수정
- `test`: 테스트 코드 추가/수정
- `chore`: 빌드, 설정 파일 수정
- `style`: 코드 포맷팅 (기능 변경 없음)

### 예시

```
feature: 채팅 메시지 읽음 처리 기능 구현

- ChatMessage에 readStatus 필드 추가
- markAsRead() 메서드 구현
- 메시지 조회 시 자동 읽음 처리 로직 추가
- 테스트 코드 작성 (Unit, Service, Integration)
```
