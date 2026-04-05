# Notio Backend

Spring Boot 4.x + Java 25 기반 백엔드 애플리케이션

## 기술 스택

- **언어**: Java 25
- **프레임워크**: Spring Boot 4.x
- **빌드**: Gradle 8.x (Kotlin DSL)
- **ORM**: Spring Data JPA + QueryDSL
- **AI**: Spring AI (Ollama)
- **데이터베이스**: PostgreSQL 16 + pgvector
- **캐시**: Redis 7
- **마이그레이션**: Flyway 10.x
- **보안**: Spring Security + JWT
- **테스트**: JUnit 5, Mockito, Testcontainers
- **문서**: SpringDoc OpenAPI (Swagger)

## 프로젝트 구조

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/notio/
│   │   │   ├── NotioApplication.java
│   │   │   ├── config/              # 설정 클래스
│   │   │   ├── notification/        # 알림 도메인
│   │   │   ├── todo/                # 할일 도메인
│   │   │   ├── chat/                # 채팅 도메인
│   │   │   ├── auth/                # 인증 도메인
│   │   │   └── common/              # 공통 유틸리티
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       └── db/migration/        # Flyway 마이그레이션
│   └── test/
│       └── java/com/notio/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 시작하기

### 사전 요구사항

- Java 25
- Docker (PostgreSQL, Redis, Ollama)

### 설치 및 실행

1. **환경 변수 설정**
```bash
# 루트 디렉토리에서
cp ../.env.example ../.env
# .env 파일 수정
```

2. **Docker 서비스 시작**
```bash
cd ..
docker-compose up -d
```

3. **애플리케이션 실행**
```bash
./gradlew bootRun
```

또는

```bash
./gradlew build
java -jar build/libs/notio-backend-0.0.1-SNAPSHOT.jar
```

### 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 코드 품질 검사
./gradlew checkstyleMain spotbugsMain

# 테스트 커버리지
./gradlew test jacocoTestReport
```

## API 문서

애플리케이션 실행 후 다음 URL에서 Swagger UI를 확인할 수 있습니다:
```
http://localhost:8080/swagger-ui.html
```

## 환경변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `NOTIO_DB_HOST` | PostgreSQL 호스트 | `localhost` |
| `NOTIO_DB_PORT` | PostgreSQL 포트 | `5432` |
| `NOTIO_DB_NAME` | 데이터베이스 이름 | `notio` |
| `NOTIO_DB_USERNAME` | DB 사용자명 | `notio` |
| `NOTIO_DB_PASSWORD` | DB 비밀번호 | `notio` |
| `NOTIO_REDIS_HOST` | Redis 호스트 | `localhost` |
| `NOTIO_REDIS_PORT` | Redis 포트 | `6379` |
| `NOTIO_OLLAMA_URL` | Ollama URL | `http://localhost:11434` |
| `NOTIO_JWT_SECRET` | JWT 시크릿 | - |
| `NOTIO_SERVER_PORT` | 서버 포트 | `8080` |

## 개발 규칙

### 패키지 구조
- 도메인별로 수평 분리 (레이어별 분리 금지)
- 예: `com.notio.notification`, `com.notio.todo`

### 네이밍 규칙
| 유형 | 규칙 | 예시 |
|------|------|------|
| Entity | 명사 단수형 | `Notification`, `Todo` |
| Repository | `{Domain}Repository` | `NotificationRepository` |
| Service | `{Domain}Service` | `NotificationService` |
| Controller | `{Domain}Controller` | `NotificationController` |
| DTO (응답) | `{Domain}Response` | `NotificationResponse` |
| DTO (요청) | `{Action}{Domain}Request` | `CreateTodoRequest` |

### 코드 스타일
- 생성자 주입 사용 (필드 주입 금지)
- `final` 키워드 적극 활용
- 레이어 분리: Controller → Service → Repository

### 테스트
- 모든 Service 계층 메서드는 단위 테스트 필수
- Testcontainers로 실제 PostgreSQL + Redis 사용
- `@WebMvcTest`로 Controller 계층 테스트

## 데이터베이스

### 마이그레이션
Flyway를 사용하여 데이터베이스 스키마를 버전 관리합니다.

```bash
# 마이그레이션 실행 (자동)
./gradlew bootRun

# 마이그레이션 정보 확인
./gradlew flywayInfo

# 마이그레이션 검증
./gradlew flywayValidate
```

### 스키마 규칙
- 모든 테이블: `id BIGSERIAL PK`, `created_at`, `updated_at`, `deleted_at` (soft delete)
- 컬럼명: `snake_case`
- 인덱스명: `idx_{테이블}_{컬럼}`
- FK명: `fk_{테이블}_{참조테이블}`

## 보안

- JWT 기반 인증
- HMAC 또는 Bearer 토큰으로 Webhook 검증
- SQL Injection 방지: Prepared Statement 또는 QueryDSL 사용
- 환경 변수로 민감 정보 관리

## 성능 최적화

- N+1 문제 방지: `@EntityGraph` 사용
- Redis 캐싱: 오늘 요약 24시간 TTL
- 페이지네이션: `Pageable` 사용 (기본 20개)
- SSE 스트리밍: 긴 AI 응답 점진적 전송

## 문의

상세한 개발 가이드는 [`../.claude/claude.md`](../.claude/claude.md)를 참조하세요.
