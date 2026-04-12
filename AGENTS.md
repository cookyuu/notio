# AGENTS.md

This file provides project instructions for Codex.

## Project overview

**Notio** is an AI-powered unified notification hub for developers.

Core goals:
- Aggregate notifications from Claude Code, Slack, GitHub, Gmail, and similar tools
- Use LLMs to summarize, prioritize, and generate actionable todos
- Provide productivity insights from notification patterns
- Support real-time push via FCM/APNs
- Maintain a polished dark UI with violet accents and glassmorphism

Detailed architecture reference: `/docs/blueprint/notio_blueprint.md`

## Architecture roadmap

- **Phase 0**: 5 weeks — Spring Boot monolith MVP
- **Phase 1**: 3–6 months — split AI service to Python
- **Phase 2**: 6–9 months — split Notification/Webhook services
- **Phase 3**: 9–12 months — split Chat/Todo services
- **Phase 4**: 12+ months — full MSA (Auth, Analytics, Gateway)

## Tech stack

### Backend
- Java 25
- Spring Boot 4.0.0
- Gradle 9.0 (Kotlin DSL)
- Spring Data JPA / Hibernate
- Redis
- PostgreSQL 16
- Flyway
- Spring Security + JWT
- Firebase Admin SDK
- JUnit 5 / Mockito / Testcontainers

### Frontend
- Flutter 3.x
- Dart 3.6.x
- Riverpod + Flutter Hooks
- Dio + Retrofit
- Drift (SQLite)
- Firebase Messaging

### AI service
- Python 3.12
- FastAPI
- Poetry
- LangChain
- Celery + Redis

### Infra
- Docker Compose for local
- Kubernetes in later phases
- Kafka in later phases
- Ollama + pgvector

## How Codex should work in this repository

- Read this file before making changes.
- Follow existing project structure and domain boundaries.
- Prefer minimal, high-confidence changes over broad rewrites.
- Do not introduce new frameworks or major dependencies unless clearly justified.
- When touching architecture-sensitive areas, preserve the current phase strategy.
- If this file becomes too large, prefer keeping this file concise and linking to task-specific docs. 

## Core engineering principles

### Backend
- Follow **clean code** and **object-oriented design**.
- Apply **single responsibility principle** to classes and methods.
- Use **constructor injection** only. Do not use field injection.
- Prefer **immutability** and `final` where practical.
- Keep controller → service → repository boundaries strict.
- Organize packages by **domain**, not by technical layer alone.
- Abstract similar behavior behind interfaces.
  - Examples: `WebhookHandler`, `WebhookVerifier`, `LlmProvider`

### Naming

#### Java
- Entity: singular noun, e.g. `Notification`, `Todo`
- Repository: `{Domain}Repository`
- Service: `{Domain}Service`
- Controller: `{Domain}Controller`
- Request DTO: `{Action}{Domain}Request`
- Response DTO: `{Domain}Response`
- Exception: `{Domain}Exception`
- Config: `{Feature}Config`
- Enum: PascalCase
- Enum values: UPPER_SNAKE_CASE
- Boolean fields: `is*`, `has*`, `can*`
- Variables: camelCase
- Constants: UPPER_SNAKE_CASE

#### Flutter
- Files: snake_case
- Screen widget: `{Feature}Screen`
- Common widget: `{Feature}Widget` or clear descriptive noun
- Entity: `{Domain}Entity`
- Model: `{Domain}Model`
- Provider: `{feature}Provider`
- Notifier: `{Feature}Notifier`
- Repository: `{Feature}Repository`
- Repository impl: `{Feature}RepositoryImpl`
- Enum: PascalCase
- Enum values: camelCase

### API design
- Use lowercase kebab-case paths
- Use plural collection names
- Represent actions with HTTP methods instead of verbs in URLs
- Exception allowed for explicit action endpoints such as `/api/v1/notifications/read-all`

Examples:
- `/api/v1/notifications`
- `/api/v1/chat/daily-summary`

## Database conventions

- Every table should include:
  - `id BIGSERIAL PK`
  - `created_at TIMESTAMPTZ`
  - `updated_at TIMESTAMPTZ`
- Use `deleted_at TIMESTAMPTZ` for soft delete
- Use `snake_case` for column names
- Index names: `idx_{table}_{column}`
- FK names: `fk_{table}_{referenced_table}`
- Default important indexes include `source`, `created_at`, and `is_read`

## API response format

Successful response:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

Error response:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "NOTIFICATION_NOT_FOUND",
    "message": "알림을 찾을 수 없습니다."
  }
}
```

## Environment variables

- All environment variables must use the `NOTIO_` prefix
- Examples:
  - `NOTIO_JWT_SECRET`
  - `NOTIO_DB_URL`
  - `NOTIO_REDIS_HOST`

## Workflow expectations

### Branch naming
- `main`
- `feat/{feature}`
- `fix/{bug}`
- `chore/{task}`
- `docs/{doc}`
- `refactor/{module}`

### Before finishing work

#### Backend
- Run tests: `./gradlew test`
- Run quality checks where configured: `./gradlew checkstyleMain spotbugsMain`
- Verify app starts: `./gradlew bootRun`
- Check API docs if enabled: `http://localhost:8080/swagger-ui.html`

#### Frontend
- `flutter analyze`
- `flutter test`
- `flutter build apk --debug`

#### General
- Add or update automated tests when behavior changes
- Confirm manual flow if the change affects user-visible functionality

## Security rules

- Never commit secrets, `.env`, service account files, or local config files
- All external webhooks must be verified with HMAC or bearer tokens
- Prevent SQL injection through prepared queries / ORM / QueryDSL
- Sanitize any HTML-rendered content
- Use a production-grade JWT secret of at least 256-bit randomness

Files that must not be committed include:

```gitignore
.env
.env.local
firebase-service-account.json
google-services.json
GoogleService-Info.plist
backend/build/
frontend/build/
*.class
*.jar
.idea/
.vscode/
*.iml
.DS_Store
Thumbs.db
```

## Performance rules

- Avoid N+1 queries; default `@OneToMany` to LAZY
- Use `@EntityGraph` or explicit fetch optimization when needed
- Cache summary-style data in Redis where appropriate
- Paginate all list APIs with sensible defaults
- Stream long AI responses with SSE
- Use efficient list rendering on Flutter

## Error handling rules

- Standardize backend errors through `GlobalExceptionHandler`
- Keep response format consistent with `ApiResponse`
- Frontend network layer should centralize auth refresh/logout and retry behavior
- Plan fallback behavior for LLM outages in Phase 1+

## Testing rules

- Service-layer methods should have unit tests
- Use Testcontainers for PostgreSQL and Redis integration tests where possible
- Use `@WebMvcTest` or equivalent slice tests for controllers
- Flutter should include widget tests; golden tests for design system components when useful

## Data and AI rules

- Manage schema changes with Flyway only; no manual ALTER in normal workflow
- `embedding` should use `vector(768)` when pgvector is enabled
- Apply soft-delete conditions consistently
- Centralize prompts with a `PromptBuilder`; do not hardcode prompts inline repeatedly
- Default RAG top-k is 5 unless changed with evidence
- Use streaming timeout/retry behavior for long-running model responses

## Frontend design system rules

- Define colors only in `AppColors`
- Define typography only in `AppTextStyles`
- Use shared spacing constants such as `AppSpacing`
- Reuse a shared `GlassCard` instead of ad hoc glassmorphism containers

## Commit message format

Format:

```text
<type>: <subject>
<body>
```

Allowed types:
- `feature`
- `fix`
- `refactor`
- `docs`
- `test`
- `chore`
- `style`

Example:

```text
feature: 채팅 메시지 읽음 처리 기능 구현

- ChatMessage에 readStatus 필드 추가
- markAsRead() 메서드 구현
- 메시지 조회 시 자동 읽음 처리 로직 추가
- 테스트 코드 작성 (Unit, Service, Integration)
```

## Priority when instructions conflict

1. Direct user request
2. More specific `AGENTS.md` in a deeper subdirectory
3. This repository-level `AGENTS.md`
4. Existing codebase conventions

## Notes for large repositories

- Keep this file as the stable top-level guide.
- Put feature-specific instructions in deeper subdirectories when needed.
- Prefer referencing detailed docs instead of duplicating large design documents here.
