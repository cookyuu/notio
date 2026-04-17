# Backend Fix 개발 체크리스트

> 대상: notifications 목록/상세 API 분리 및 상세 조회 시 읽음 처리 반영
> 범위: Spring Boot 4.x · Java 25 · Notification API

---

## Phase 0. 계약 재정의

- [x] `GET /api/v1/notifications`를 목록 전용 API로 재정의한다.
- [x] `GET /api/v1/notifications/{id}`를 상세 전용 API로 재정의한다.
- [x] 목록 응답에는 카드 렌더링에 필요한 최소 필드만 포함하도록 기준을 확정한다.
- [x] 상세 응답에는 전체 본문과 부가 정보(`external_url`, `metadata` 등)를 유지하도록 기준을 확정한다.
- [x] 상세 조회 시 미읽음 알림이면 읽음 상태로 변경하는 것을 서버 계약으로 고정한다.

## Phase 1. 목록/상세 DTO 분리

- [x] 목록 전용 DTO를 추가한다.
- [x] 목록 DTO에는 `id`, `source`, `title`, `priority`, `is_read`, `created_at`, `body_preview`를 포함한다.
- [x] 상세 전용 DTO는 기존 전체 필드 구조를 유지하거나 동일 목적의 DTO로 명확히 분리한다.
- [x] 목록 응답에서 `body`, `external_url`, `metadata`, `updated_at`, `external_id`, `connection_id`를 제외한다.
- [x] `body_preview` 생성 규칙을 서버에서 일관되게 처리하도록 정리한다.

## Phase 2. Controller 응답 분리

- [x] `NotificationController.getNotifications()`가 목록 DTO 페이지를 반환하도록 변경한다.
- [x] `NotificationController.getNotification()`가 상세 DTO를 반환하도록 정리한다.
- [x] 목록 API와 상세 API가 동일한 사용자 스코프 규칙을 사용하도록 확인한다.
- [x] 상세 조회 엔드포인트 설명과 Swagger 문구를 새 계약 기준으로 정리한다.

## Phase 3. Service 상세 조회 읽음 처리 정리

- [x] 상세 조회 전용 service 메서드를 명확히 분리한다.
- [x] 상세 조회 시 대상 알림이 미읽음이면 읽음 상태로 전환한 뒤 반환하도록 구현한다.
- [x] 이미 읽은 알림이면 상태 변경 없이 상세 데이터만 반환하도록 구현한다.
- [x] 실제 읽음 전환이 발생한 경우 unread count cache eviction이 보장되도록 정리한다.
- [x] 상세 조회 로직이 별도 `PATCH /notifications/{id}/read` 계약과 충돌하지 않도록 정리한다.

## Phase 4. 목록 응답 최적화

- [x] 목록 API가 화면에 불필요한 전체 본문/메타데이터를 조회·직렬화하지 않도록 정리한다.
- [x] 필요 시 repository projection 또는 DTO 매핑 단에서 목록 응답을 경량화한다.
- [x] 목록 API 변경이 페이지네이션 구조(`content`, `page`, `size`)를 깨지 않도록 유지한다.
- [x] 프론트가 사용하는 목록 정렬 기준(`createdAt desc`)을 유지한다.

## Phase 5. 테스트 및 검증

- [x] 목록 API controller test를 추가하거나 보강한다.
- [ ] 목록 API가 목록 DTO 필드만 반환하는지 검증한다.
- [ ] 상세 API 호출 시 미읽음 알림이 읽음으로 바뀌고 `is_read=true`로 응답되는지 테스트한다.
- [ ] 상세 API 호출 시 이미 읽은 알림은 상태 변화 없이 반환되는지 테스트한다.
- [x] `body_preview` 생성 규칙 테스트를 추가한다.
- [x] `NotificationService` 단위 테스트에서 unread count cache eviction 조건을 검증한다.
- [ ] `./gradlew test`를 실행해 관련 테스트를 검증한다.
