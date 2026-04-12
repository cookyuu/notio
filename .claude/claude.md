# Claude Code 개발 가이드

## 프로젝트 개요

**Notio**는 개발자를 위한 AI 기반 통합 알림 허브입니다.

### 핵심 기능
- 🔔 **다중 소스 통합**: Claude Code, Slack, GitHub, Gmail 등 개발 도구 알림을 한 곳에서 관리
- 🤖 **AI 자동화**: LLM 기반 알림 요약, 우선순위 분류, 할일 자동 생성
- 📊 **패턴 분석**: 알림 패턴 분석으로 생산성 인사이트 제공
- 📱 **실시간 푸시**: FCM/APNs를 통한 즉시 알림
- 🌙 **세련된 UI**: 다크 모드 + 바이올렛 액센트의 글래스모피즘 디자인

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
### API 명세서

API 명세서는 /docs/api/spec.md 참조

---

## 기술 스택과 버전

### Backend (Spring Boot)
| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 25 |
| 프레임워크 | Spring Boot | 4.0.0 |
| 빌드 | Gradle | 9.0 (Kotlin DSL) |
| ORM | Spring Data JPA | 7.1.x (Hibernate) |
| AI | Spring AI (Ollama) | 1.0.0-M6 (Phase 1+) |
| 캐시/큐 | Spring Data Redis | 3.x |
| DB | PostgreSQL 16 | — |
| 마이그레이션 | Flyway | 11.1.0 |
| 보안 | Spring Security + JWT (jjwt) | 0.12.6 |
| 푸시 | Firebase Admin SDK | 9.4.2 |
| 테스트 | JUnit 5 · Mockito · Testcontainers | 1.20.4 |
| 문서 | Swagger (Springfox) | 3.0.0 |

### Frontend (Flutter)
| 분류 | 패키지 | 버전 |
|------|--------|------|
| 언어 | Dart | 3.6.x |
| 프레임워크 | Flutter | 3.x |
| 상태관리 | hooks_riverpod + flutter_hooks | 2.5.3 / 0.20.5 |
| 코드 생성 | riverpod_generator + build_runner | 2.6.2 / 2.4.13 |
| 네비게이션 | go_router | 14.6.1 |
| 네트워크 | dio + retrofit | 5.4.3 / 4.1.0 |
| JSON | json_annotation + json_serializable | 4.9.0 / 6.8.0 |
| 로컬 DB | drift (SQLite) + drift_dev | 2.18.0 |
| 푸시 알림 | firebase_core + firebase_messaging | 2.27.2 / 14.7.20 |
| 로컬 알림 | flutter_local_notifications | 17.2.3 |
| 리스트 제스처 | flutter_slidable | 3.1.1 |
| 차트 | fl_chart | 0.68.0 |
| 보안 스토리지 | flutter_secure_storage | 9.2.2 |
| 시간 포맷 | timeago | 3.6.1 |
| Lint | flutter_lints + custom_lint + riverpod_lint | 5.0.0 / 0.6.7 / 2.5.1 |

### AI Service (Phase 1+)
| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Python | 3.12 |
| 프레임워크 | FastAPI | 0.115.x |
| 패키지 관리 | Poetry | 1.8.x |
| RAG | LangChain | 0.3.x |
| 비동기 태스크 | Celery + Redis | 5.x |
| 테스트 | pytest + httpx | — |

### Infrastructure
| 분류 | 기술 | 버전 |
|------|------|------|
| 로컬 환경 | Docker Compose | 3.8 |
| 프로덕션 | Kubernetes | — (Phase 2+) |
| 메시지 큐 | Apache Kafka | 3.6 (Phase 2+) |
| 캐시/브로커 | Redis | 7-alpine |
| DB | PostgreSQL (ankane/pgvector) | 16 (v0.5.1) |
| LLM | Ollama (llama3.2:3b) | latest |
| 임베딩 | Ollama (nomic-embed-text) | latest |

---

## 개발 규칙

### Backend 개발 원칙

**클린 코드 & 객체지향 설계**
- **단일 책임 원칙(SRP)**: 각 클래스는 하나의 명확한 책임만 가짐
- **전략 패턴**: 유사한 동작의 다양한 구현체는 인터페이스로 추상화
  - 예: `WebhookHandler`, `WebhookVerifier`, `LlmProvider`
- **의존성 주입**: 생성자 주입 사용, 필드 주입 금지
- **불변성 우선**: `final` 키워드 적극 활용, 가변 상태 최소화
- **명확한 네이밍**: 메서드명에서 행위가 명확히 드러나도록 작성
  - `find*`: 조회, `save`/`create`: 저장/생성, `update`: 수정, `delete`: 삭제
- **레이어 분리**: Controller → Service → Repository 계층 엄격히 준수
- **도메인 우선**: 패키지는 도메인별로 수평 분리 (레이어별 분리 금지)

### 네이밍 규칙

**Backend (Java)**
| 유형 | 규칙 | 예시 |
|------|------|------|
| Entity | 명사 단수형 | `Notification`, `Todo` |
| Repository | `{Domain}Repository` | `NotificationRepository` |
| Service | `{Domain}Service` | `NotificationService` |
| Controller | `{Domain}Controller` | `NotificationController` |
| DTO (응답) | `{Domain}Response` | `NotificationResponse` |
| DTO (요청) | `{Action}{Domain}Request` | `CreateTodoRequest` |
| Interface | 형용사 또는 동사 | `WebhookHandler`, `WebhookVerifier` |
| Enum | PascalCase | `NotificationSource`, `TodoStatus` |
| Enum 값 | UPPER_SNAKE | `CLAUDE`, `IN_PROGRESS` |
| Exception | `{Domain}Exception` | `NotificationNotFoundException` |
| Config | `{기능}Config` | `SecurityConfig`, `RedisConfig` |
| 변수 | camelCase | `notificationId` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Boolean | `is`, `has`, `can` 접두사 | `isRead`, `hasEmbedding` |

**Frontend (Flutter)**
| 유형 | 규칙 | 예시 |
|------|------|------|
| 파일명 | snake_case | `notification_card.dart` |
| 화면 Widget | `{Feature}Screen` | `NotificationsScreen` |
| 일반 Widget | `{기능}Widget` 또는 `{기능}` | `GlassCard` |
| Entity | `{Domain}Entity` | `NotificationEntity` |
| Model (DTO) | `{Domain}Model` | `NotificationModel` |
| Provider | `{feature}Provider` (camelCase) | `notificationsProvider` |
| Notifier | `{Feature}Notifier` | `NotificationsNotifier` |
| Repository | `{Feature}Repository` | `NotificationRepository` |
| Repository (impl) | `{Feature}RepositoryImpl` | `NotificationRepositoryImpl` |
| Enum | PascalCase | `NotificationSource` |
| Enum 값 | camelCase | `claude`, `inProgress` |
| 변수 | camelCase | `unreadCount` |
| 상수 | lowerCamelCase | `const baseUrl = '...'` |
| private | `_camelCase` | `_fetchNotifications` |

**API 엔드포인트**
- 소문자 케밥 케이스: `/api/v1/notifications`, `/api/v1/chat/daily-summary`
- 컬렉션은 복수형: `/notifications`, `/todos`, `/devices`
- 동사는 HTTP 메서드로 표현 (URL에 동사 사용 금지)
- 예외: 액션 엔드포인트 `/api/v1/notifications/read-all` (PATCH)

**패키지/라우트**
- Backend 패키지: 모두 소문자 (`com.notio.notification`, `com.notio.webhook`)
- Frontend 라우트: `/notifications`, `/chat`, `/analytics`, `/settings`

### DB 스키마 규칙

- 모든 테이블: `id BIGSERIAL PK`, `created_at TIMESTAMPTZ`, `updated_at TIMESTAMPTZ`
- 삭제: `deleted_at TIMESTAMPTZ` soft delete
- 컬럼명: `snake_case`
- 인덱스명: `idx_{테이블}_{컬럼}` (예: `idx_notifications_source`)
- FK명: `fk_{테이블}_{참조테이블}` (예: `fk_todos_notifications`)

### 공통 응답 형식 (Backend)

```java
// 성공 시
{
  "success": true,
  "data": { ... },
  "error": null
}

// 에러 시
{
  "success": false,
  "data": null,
  "error": {
    "code": "NOTIFICATION_NOT_FOUND",
    "message": "알림을 찾을 수 없습니다."
  }
}
```

### 환경변수 규칙

- 모든 환경변수는 `NOTIO_` 접두사 사용
- 예: `NOTIO_JWT_SECRET`, `NOTIO_DB_URL`, `NOTIO_REDIS_HOST`

---

## 개발 워크플로우

### 브랜치 전략

```
main                    # 프로덕션 브랜치
└── feat/{feature}      # 새 기능
└── fix/{bug}           # 버그 수정
└── chore/{task}        # 빌드, 설정
└── docs/{doc}          # 문서 작성
└── refactor/{module}   # 리팩토링
```

### 커밋 전 체크리스트

**Backend**
- [ ] 테스트 실행: `./gradlew test`
- [ ] 코드 품질: `./gradlew checkstyleMain spotbugsMain`
- [ ] Spring Boot 실행 확인: `./gradlew bootRun`
- [ ] API 문서 확인: `http://localhost:8080/swagger-ui.html`

**Frontend**
- [ ] 분석: `flutter analyze` (경고 0개)
- [ ] 테스트: `flutter test`
- [ ] 빌드: `flutter build apk --debug`
- [ ] 린트: `flutter_lints` 규칙 통과

**Test**
- [ ] 통합 테스트 작성
- [ ] 수동 테스트 완료

## 관련 이슈
Closes #42
```

---

## 특별 주의사항

### 보안

- **절대 커밋 금지**: `.env`, `firebase-service-account.json`, `*.local`, API 키
- **JWT 시크릿**: 프로덕션 환경에서는 최소 256비트 랜덤 문자열 사용
- **Webhook 검증 필수**: 모든 외부 webhook은 HMAC 또는 Bearer 토큰으로 검증
- **SQL Injection 방지**: 모든 쿼리는 Prepared Statement 또는 QueryDSL 사용
- **XSS 방지**: Flutter는 기본적으로 안전하지만, HTML 렌더링 시 sanitize 필수

### 성능

- **N+1 문제 방지**: JPA에서 `@OneToMany`는 `fetch = FetchType.LAZY` 사용, 필요시 `@EntityGraph`
- **Redis 캐싱**: 오늘 요약은 24시간 TTL, 미읽음 수는 실시간 갱신
- **페이지네이션**: 모든 목록 조회는 `Pageable` 사용 (기본 20개)
- **SSE 스트리밍**: 긴 AI 응답은 SSE로 점진적 전송
- **무한 스크롤**: Flutter는 `ListView.builder` + `ScrollController`

### 에러 처리

- **Backend**: `GlobalExceptionHandler`에서 모든 예외를 `ApiResponse` 형식으로 통일
- **Frontend**: `DioClient`의 `AuthInterceptor`에서 401 → 로그아웃, 5xx → 재시도
- **LLM 장애**: Ollama 장애 시 Claude API Fallback (Phase 1+)

### 테스트

- **단위 테스트**: 모든 Service 계층 메서드는 단위 테스트 필수
- **통합 테스트**: Testcontainers로 실제 PostgreSQL + Redis 사용
- **슬라이스 테스트**: `@WebMvcTest`로 Controller 계층 테스트
- **Flutter**: Widget 테스트 + 골든 테스트 (디자인 시스템 위젯)

### 데이터베이스

- **마이그레이션**: Flyway로 버전 관리, 절대 수동 ALTER 금지
- **인덱스**: `source`, `created_at`, `is_read` 컬럼은 인덱스 필수
- **pgvector**: `embedding` 컬럼은 `vector(768)` 타입, cosine distance 인덱스 생성
- **Soft Delete**: `deleted_at IS NULL` 조건은 모든 쿼리에 자동 적용

### AI/LLM

- **프롬프트**: `PromptBuilder`로 중앙 관리, 하드코딩 금지
- **RAG top-k**: 기본 5개, 성능 모니터링 후 조정
- **임베딩 차원**: 768 (nomic-embed-text 고정)
- **스트리밍 타임아웃**: 30초 초과 시 재연결 (Exponential Backoff)

### 디자인 시스템 (Frontend)

- **색상**: `AppColors`에서만 정의, 하드코딩 금지
- **타이포그래피**: `AppTextStyles`에서만 정의
- **간격**: `AppSpacing` 상수 사용 (s4, s8, s12, s16, s20, s24, s32)
- **글래스모피즘**: `GlassCard` 위젯 재사용, 직접 `Container` 작성 금지

### Git 커밋 금지 항목

```gitignore
# 환경변수
.env
.env.local

# Firebase
firebase-service-account.json
google-services.json
GoogleService-Info.plist

# 빌드 결과물
backend/build/
frontend/build/
*.class
*.jar

# IDE
.idea/
.vscode/
*.iml

# OS
.DS_Store
Thumbs.db
```

---

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
