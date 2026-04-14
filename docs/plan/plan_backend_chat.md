# Chat 메시지 영구 저장 기능 구현 계획

## 개요

현재 Chat 도메인은 메모리 기반(`List<ChatMessageResponse>`)으로 서버 재시작 시 모든 대화 기록이 손실됩니다. 본 계획은 PostgreSQL 기반 영구 저장으로 전환하고, 90일 자동 보관 정책을 적용합니다.

**설계 원칙:**
- Notification/Todo 도메인과 동일한 패턴 적용
- Lombok 기반 Entity 설계 (Notification 스타일)
- Soft Delete 지원 (90일 후 자동 삭제)
- 페이지네이션 지원

---

## 구현 단계

### 1. ChatMessage 엔티티 생성

**파일:** `backend/src/main/java/com/notio/chat/domain/ChatMessage.java` (신규)

**필드:**
- `id`: BIGSERIAL PK
- `userId`: Long (nullable, Phase 0 단일 사용자)
- `role`: ChatRole ENUM (USER, ASSISTANT)
- `content`: TEXT (AI 응답 길이 고려)
- `createdAt`, `updatedAt`: Instant (@CreationTimestamp/@UpdateTimestamp)
- `deletedAt`: Instant (Soft Delete)

**인덱스:**
- `user_id`, `role`, `created_at DESC`
- 조건부 인덱스: `WHERE deleted_at IS NULL`

**참고:** `backend/src/main/java/com/notio/notification/domain/Notification.java`

---

### 2. ChatRole ENUM 생성

**파일:** `backend/src/main/java/com/notio/chat/domain/ChatRole.java` (신규)

```java
public enum ChatRole {
    USER,      // 사용자 메시지
    ASSISTANT  // AI 응답
}
```

---

### 3. ChatMessageRepository 생성

**파일:** `backend/src/main/java/com/notio/chat/repository/ChatMessageRepository.java` (신규)

**주요 메서드:**
- `findAllNotDeleted(Pageable)`: 전체 대화 기록 조회 (페이지네이션)
- `findAllWithFilter(ChatRole, Pageable)`: 역할별 필터링
- `findOldMessages(Instant threshold)`: 90일 이전 메시지 조회
- `softDeleteOldMessages(Instant threshold)`: 일괄 Soft Delete (@Modifying)
- `countNotDeleted()`: 전체 메시지 개수

**참고:** `backend/src/main/java/com/notio/notification/repository/NotificationRepository.java`

---

### 4. Flyway 마이그레이션 생성

**파일:** `backend/src/main/resources/db/migration/V4__create_chat_messages_table.sql` (신규)

**DDL:**
```sql
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

-- 조건부 인덱스 (Soft Delete 제외)
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_chat_messages_role ON chat_messages(role) WHERE deleted_at IS NULL;
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at DESC) WHERE deleted_at IS NULL;
```

**주의:** V2 마이그레이션에서 `chat_messages` 테이블을 참조하지만 실제로는 생성되지 않았음. V4에서 신규 생성.

---

### 5. ChatService 리팩토링

**파일:** `backend/src/main/java/com/notio/chat/service/ChatService.java` (수정)

**변경 사항:**
1. **메모리 저장 제거**: `List<ChatMessageResponse> history`, `AtomicLong sequence` 삭제
2. **ChatMessageRepository 주입**: 생성자에 추가
3. **chat() 메서드 수정**:
   - 사용자 메시지 DB 저장 (`ChatRole.USER`)
   - AI 응답 생성 (기존 `generateDummyAiResponse()` 유지)
   - AI 응답 DB 저장 (`ChatRole.ASSISTANT`)
4. **streamChat() 메서드 수정**:
   - SSE 스트리밍 완료 후 AI 응답 저장
5. **history() 메서드 수정**:
   - `chatMessageRepository.findAllNotDeleted()` 호출
   - 페이지네이션 지원 오버로딩: `history(Pageable)`
   - 기존 API 호환성: `history()` → 최근 100개 반환
6. **헬퍼 메서드 추가**:
   - `saveMessage(ChatRole, String)`: 메시지 저장
   - `toResponse(ChatMessage)`: Entity → DTO 변환

**트랜잭션:** `@Transactional` 적용

**참고:** `backend/src/main/java/com/notio/notification/service/NotificationService.java`

---

### 6. ChatController 수정

**파일:** `backend/src/main/java/com/notio/chat/controller/ChatController.java` (수정)

**변경 사항:**
- 기존 엔드포인트 유지 (호환성)
- **선택사항:** 페이지네이션 엔드포인트 추가
  - `GET /api/v1/chat/history/page?page=0&size=20`
  - `ApiResponse<Page<ChatMessageResponse>>` 반환

---

### 7. 자동 삭제 스케줄러 구현

**파일:** `backend/src/main/java/com/notio/chat/scheduler/ChatMessageCleanupScheduler.java` (신규)

**기능:**
- 매일 새벽 3시 (KST) 실행 → Cron: `0 0 18 * * ?` (UTC)
- 90일 이전 메시지 Soft Delete
- `chatMessageRepository.softDeleteOldMessages()` 호출

**Spring Scheduling 활성화:**
- `backend/src/main/java/com/notio/NotioApplication.java`
- `@EnableScheduling` 어노테이션 추가

---

### 8. 설정 파일 (선택사항)

**파일:** `backend/src/main/resources/application.yml` (수정)

```yaml
notio:
  chat:
    retention-days: 90  # 보관 기간 (일 단위)
```

**Properties 클래스:** `backend/src/main/java/com/notio/chat/config/ChatProperties.java` (신규)

---

## 주요 파일 목록

### 신규 생성 (6개)
1. `backend/src/main/java/com/notio/chat/domain/ChatMessage.java`
2. `backend/src/main/java/com/notio/chat/domain/ChatRole.java`
3. `backend/src/main/java/com/notio/chat/repository/ChatMessageRepository.java`
4. `backend/src/main/resources/db/migration/V4__create_chat_messages_table.sql`
5. `backend/src/main/java/com/notio/chat/scheduler/ChatMessageCleanupScheduler.java`
6. `backend/src/main/java/com/notio/chat/config/ChatProperties.java` (선택사항)

### 수정 (2개)
1. `backend/src/main/java/com/notio/chat/service/ChatService.java` (대규모 리팩토링)
2. `backend/src/main/java/com/notio/NotioApplication.java` (@EnableScheduling 추가)

### 선택 수정 (1개)
1. `backend/src/main/java/com/notio/chat/controller/ChatController.java` (페이지네이션 엔드포인트)

---

## 테스트 계획

### 단위 테스트
- **ChatServiceTest**: `chat()`, `history()` 메서드 동작 확인
- **ChatMessageRepositoryTest**: Soft Delete 쿼리, 90일 필터링 확인

### 통합 테스트 (Testcontainers)
- PostgreSQL 컨테이너로 전체 플로우 테스트
- 채팅 전송 → DB 저장 → 조회 확인
- 스케줄러 동작 확인

### 성능 테스트
- 10,000개 메시지 저장 후 페이지네이션 조회 속도
- 인덱스 사용 확인: `EXPLAIN ANALYZE`

---

## 마이그레이션 영향도

### 기존 데이터 처리
- 현재 메모리에만 존재하는 데이터는 폐기 (휘발성이므로 손실 없음)
- V4 마이그레이션으로 빈 `chat_messages` 테이블 생성
- 새로운 대화부터 DB 저장 시작

### 다운타임
- **Zero Downtime 가능**
- Flyway 자동 마이그레이션 (테이블 생성만, 락 없음)
- 서비스 연속성 유지

### 롤백 계획
- 애플리케이션 롤백 → 이전 버전 배포 (메모리 기반)
- DB 롤백 → `DROP TABLE chat_messages` + Flyway 히스토리 삭제

---

## 구현 체크리스트

### Phase 1: 기본 구조
- [ ] `ChatRole` ENUM 생성
- [ ] `ChatMessage` 엔티티 생성
- [ ] `ChatMessageRepository` 생성
- [ ] Flyway V4 마이그레이션 작성

### Phase 2: 서비스 리팩토링
- [ ] `ChatService` 메모리 → DB 전환
- [ ] `chat()` 메서드 수정
- [ ] `streamChat()` 메서드 수정
- [ ] `history()` 메서드 수정 (페이지네이션)

### Phase 3: 자동 삭제
- [ ] `ChatMessageCleanupScheduler` 구현
- [ ] `@EnableScheduling` 활성화
- [ ] `ChatProperties` 설정 (선택사항)

### Phase 4: 테스트
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] 성능 테스트 실행

### Phase 5: 배포
- [ ] 로컬 환경 테스트
- [ ] Flyway 마이그레이션 검증
- [ ] API 동작 확인 (Swagger UI)
- [ ] 프로덕션 배포

---

## 주의사항

1. **V2 마이그레이션 충돌**: V2에서 `chat_messages` 테이블 참조하지만 실제로는 생성되지 않음. V4에서 신규 생성하면 해결됨.
2. **트랜잭션 범위**: SSE 스트리밍 비동기 스레드에서 메시지 저장 시 트랜잭션 처리 확인.
3. **타임존**: Entity는 `Instant` (UTC), DTO는 `OffsetDateTime` (클라이언트 호환성).
4. **성능**: 인덱스 활용 확인, 페이지네이션 필수.

---

## 향후 확장 (Phase 1+)

- 멀티 유저 지원: `user_id` 필드 활용
- RAG 컨텍스트: 대화 기록 임베딩 (pgvector)
- 대화방(Thread): `thread_id` 필드 추가
- 메시지 피드백: `feedback` 필드 추가 (👍/👎)
