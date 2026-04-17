# Backend Fix 개발 체크리스트

> 대상: Analytics 화면의 개인별 알림 요약 스코프 오류 수정
> 범위: Spring Boot 4.x · Java 25 · Analytics API 사용자별 집계 보장

---

## Phase 0. 현상 고정 및 영향 범위 확인

- [x] `AnalyticsController`가 현재 인증 사용자 정보를 사용하지 않는 구조인지 확인한다.
- [x] `AnalyticsService`가 사용자 ID 없이 `NotificationService`를 호출하는지 확인한다.
- [x] `NotificationService`의 deprecated 기본 사용자 조회 경로를 Analytics가 타고 있는지 확인한다.
- [x] Analytics 응답 필드(`totalNotifications`, `unreadNotifications`, `sourceDistribution`, `priorityDistribution`, `dailyTrend`, `insight`)가 모두 동일 스코프에서 계산되는지 점검한다.
- [x] 이번 수정 범위를 backend 한정으로 유지하고, frontend는 회귀 확인만 수행하는 것으로 정리한다.

## Phase 1. Controller 사용자 스코프 적용

- [x] `AnalyticsController`에서 `Authentication`을 받아 현재 사용자 ID를 추출하도록 수정한다.
- [x] 인증 정보가 없거나 principal 파싱이 실패하면 `UNAUTHORIZED`를 반환하도록 기존 규칙과 맞춘다.
- [x] `GET /api/v1/analytics/weekly`가 `analyticsService.getWeeklySummary(userId)`를 호출하도록 변경한다.
- [x] Analytics 엔드포인트가 기존과 동일하게 인증이 필요한 API로 유지되는지 확인한다.

## Phase 2. Service 집계 로직 사용자별로 수정

- [x] `AnalyticsService` 메서드 시그니처를 `getWeeklySummary(Long userId)`로 변경한다.
- [x] 알림 조회 시 `notificationService.findAll(userId, source, isRead, pageable)` 사용자 스코프 오버로드만 사용하도록 수정한다.
- [x] 최근 7일 필터가 사용자별 알림 집합에 대해서만 적용되도록 정리한다.
- [x] source 분포 집계가 해당 사용자 알림만 반영하도록 검증한다.
- [x] priority 분포 집계가 해당 사용자 알림만 반영하도록 검증한다.
- [x] daily trend 집계가 해당 사용자 알림만 반영하도록 검증한다.
- [x] unread count 계산이 해당 사용자 알림만 반영하도록 검증한다.
- [x] insight 문구가 해당 사용자 집계 결과를 기준으로 생성되도록 정리한다.
- [x] 신규 코드에서 deprecated 기본 사용자 조회 메서드를 더 이상 사용하지 않도록 정리한다.

## Phase 3. 테스트 보강

- [x] `AnalyticsService` 단위 테스트를 추가한다.
- [x] 서로 다른 `userId`를 가진 알림이 섞여 있을 때 요청 사용자 데이터만 집계되는지 테스트한다.
- [x] 최근 7일 밖의 알림이 집계에서 제외되는지 테스트한다.
- [x] 미읽음 개수가 요청 사용자 기준으로만 계산되는지 테스트한다.
- [x] source/priority/daily trend 집계가 요청 사용자 기준으로만 계산되는지 테스트한다.
- [x] 알림이 없을 때 빈 주간 요약과 기본 insight 문구를 반환하는지 테스트한다.
- [x] `AnalyticsController` 슬라이스 또는 통합 테스트를 추가한다.
- [x] 인증 principal에 따라 다른 주간 요약 결과가 반환되는지 테스트한다.
- [x] 인증 정보가 없을 때 `401 UNAUTHORIZED`가 반환되는지 테스트한다.

## Phase 4. 회귀 검증 및 마무리

- [x] Analytics API 응답 스키마가 기존 프론트 계약과 동일한지 확인한다.
- [x] Notifications API와 Analytics API의 사용자 스코프 처리 방식이 일관적인지 점검한다.
- [ ] `./gradlew test`를 실행해 관련 테스트가 통과하는지 확인한다.
- [ ] 필요 시 `./gradlew bootRun`으로 서버 기동 후 `/api/v1/analytics/weekly`를 사용자별로 수동 검증한다.
- [ ] 수정 결과를 기준으로 향후 공통 `currentUserId` 추출 유틸 리팩터링 필요 여부를 후속 작업으로 분리한다.

`참고:` Analytics 관련 신규 테스트는 통과했지만, 전체 `./gradlew test`는 기존 `AuthPublicEndpointSecurityTest` 실패로 아직 완료 처리하지 않았다.
