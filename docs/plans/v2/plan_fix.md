# AI Token Usage Analytics — Implementation Plan

## Context

Claude Code의 작업 완료 웹훅(`send-stop-webhook.sh`)이 이미 `model`, `usage.input_tokens`, `usage.output_tokens`를 백엔드로 전송하고 있으나, 현재는 `notifications.metadata` JSONB 컬럼에 통째로 저장될 뿐 분석에 활용되지 않는다. 목표: 전용 테이블에 토큰 사용량을 저장하고, **granularity(일별/주별/월별) + 사용자 지정 날짜 범위** 조합 집계 API를 추가하며, Flutter Analytics 화면에 "AI 토큰" 탭을 신설한다.

---

## Phase 1 — Backend

### 1. V15 Migration (`V15__create_ai_usage_logs.sql`)

```sql
CREATE TABLE ai_usage_logs (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    notification_id  BIGINT       NOT NULL,
    model            VARCHAR(100) NOT NULL,
    input_tokens     BIGINT       NOT NULL DEFAULT 0,
    output_tokens    BIGINT       NOT NULL DEFAULT 0,
    total_tokens     BIGINT       GENERATED ALWAYS AS (input_tokens + output_tokens) STORED,
    session_at       TIMESTAMPTZ  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT fk_ai_usage_logs_users          FOREIGN KEY (user_id)         REFERENCES users(id),
    CONSTRAINT fk_ai_usage_logs_notifications  FOREIGN KEY (notification_id) REFERENCES notifications(id)
);

CREATE INDEX idx_ai_usage_logs_user_session_at ON ai_usage_logs(user_id, session_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_ai_usage_logs_model           ON ai_usage_logs(user_id, model)           WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_ai_usage_logs_notification_id ON ai_usage_logs(notification_id)    WHERE deleted_at IS NULL;
```

설계 포인트:
- `session_at`: webhook payload의 `notification.timestamp` 파싱값 (파싱 실패 시 `notification.created_at` fallback)
- `total_tokens`: DB generated stored column → 집계 쿼리 단순화
- unique index on `notification_id`: Branch D 중복 실행 방지(idempotency)

---

### 2. 새 파일 — `com.notio.analytics.domain`

#### `AiUsageLog.java` (Entity)
필드: `id`, `userId`, `notificationId`, `model`, `inputTokens`, `outputTokens`, `totalTokens`(삽입·수정 불가), `sessionAt`, `createdAt`, `updatedAt`, `deletedAt`
- Lombok `@Builder`, `@NoArgsConstructor(PROTECTED)`, `@Getter`
- `softDelete()` 메서드 포함

#### `AiUsageLogRepository.java` (JPA Repository)

```java
// Projection interfaces
interface AiUsageDataPoint { String getPeriodLabel(); long getTotalInput(); long getTotalOutput(); long getSessionCount(); }
interface ModelUsageDataPoint { String getModel(); long getTotalTokens(); long getSessionCount(); }

// 집계 쿼리 (native SQL — PostgreSQL to_char 사용)
@Query(nativeQuery=true) List<AiUsageDataPoint> findDailyInRange(Long userId, Instant since, Instant until);
@Query(nativeQuery=true) List<AiUsageDataPoint> findWeeklyInRange(Long userId, Instant since, Instant until);
@Query(nativeQuery=true) List<AiUsageDataPoint> findMonthlyInRange(Long userId, Instant since, Instant until);
@Query(nativeQuery=true) List<ModelUsageDataPoint> findModelDistributionInRange(Long userId, Instant since, Instant until);

// JPQL 단순 집계
long sumInputTokensInRange(Long userId, Instant since, Instant until);   // @Query
long sumOutputTokensInRange(Long userId, Instant since, Instant until);  // @Query
long countSessionsInRange(Long userId, Instant since, Instant until);    // @Query
boolean existsByNotificationId(Long notificationId);                     // 파생 메서드
```

Period → label 포맷 (native SQL `to_char`):
| Period  | 포맷 문자열          | 예시           |
|---------|---------------------|----------------|
| DAILY   | `'YYYY-MM-DD'`      | `2026-05-15`   |
| WEEKLY  | `'IYYY-"W"IW'`      | `2026-W20`     |
| MONTHLY | `'YYYY-MM'`         | `2026-05`      |

집계 범위: `startDate` / `endDate` 파라미터로 자유 지정 (날짜 범위 + granularity 분리)

최대 허용 범위 (서버 검증):
| Granularity | 최대 범위 | 기본값 (파라미터 없을 때) |
|-------------|-----------|--------------------------|
| DAILY       | 90일      | 최근 7일                 |
| WEEKLY      | 365일     | 최근 56일 (8주)          |
| MONTHLY     | 730일     | 최근 365일 (12개월)      |

#### `AiUsageGranularity.java` (Enum)
```java
public enum AiUsageGranularity { DAILY, WEEKLY, MONTHLY;
    public static AiUsageGranularity from(String value) { /* valueOf or throw NotioException(INVALID_REQUEST) */ }
    public int maxDays() { return switch(this) { case DAILY -> 90; case WEEKLY -> 365; case MONTHLY -> 730; }; }
    public int defaultDays() { return switch(this) { case DAILY -> 7; case WEEKLY -> 56; case MONTHLY -> 365; }; }
}
```

#### `AiUsageResponse.java` (Record DTO)
```java
public record AiUsageResponse(
    String granularity,
    String startDate,                     // "2026-04-01" (요청된 실제 범위)
    String endDate,                       // "2026-05-15"
    long totalInputTokens, long totalOutputTokens, long totalSessions,
    String mostUsedModel,                 // null when no data
    List<AiUsagePeriodPoint> trend,
    List<AiUsageModelShare> modelDistribution
) {
    public record AiUsagePeriodPoint(String label, long inputTokens, long outputTokens, long sessions) {}
    public record AiUsageModelShare(String model, long totalTokens, long sessions) {}
}
```

#### `AiUsageLogService.java` (Service)

**`logFromNotification(Notification saved)`** — Branch D에서 호출:
1. `saved.getSource() != CLAUDE` → 즉시 return
2. `existsByNotificationId` 체크 → 중복이면 return
3. `metadata` JSON 파싱 → `usage.input_tokens`, `usage.output_tokens`, `model` 추출
4. 둘 다 0이면 return (fallback 페이로드 케이스)
5. `AiUsageLog` 빌드 후 save

**`getAiUsage(Long userId, AiUsageGranularity granularity, LocalDate startDate, LocalDate endDate)`**:
1. `startDate` / `endDate` null이면 `granularity.defaultDays()`로 자동 설정
2. `endDate - startDate > granularity.maxDays()` → `NotioException(INVALID_REQUEST)`
3. `startDate > endDate` → `NotioException(INVALID_REQUEST)`
4. `since = startDate.atStartOfDay(ZoneOffset.UTC).toInstant()`, `until = endDate.plusDays(1).atStartOfDay(...).toInstant()` 로 변환
5. 총합/세션 수 조회 (범위 기반 `InRange` 쿼리)
6. 트렌드 조회 (granularity별 네이티브 쿼리)
7. 모델 분포 조회
8. `AiUsageResponse` 조립

---

### 3. 수정 파일

#### `NotificationService.java`
`AiUsageLogService` 생성자 주입 추가.
`saveNotification()` 내부 Branch C 아래에 **Branch D** 추가:
```java
// Branch D: AI 토큰 사용량 로깅 (비동기)
CompletableFuture.runAsync(() -> {
    try {
        aiUsageLogService.logFromNotification(saved);
    } catch (Exception e) {
        log.warn("event=ai_usage_log_failed notification_id={} user_id={} exception_type={}",
            saved.getId(), saved.getUserId(), e.getClass().getSimpleName());
    }
}, VIRTUAL_THREAD_EXECUTOR);
```

#### `AnalyticsController.java`
`AiUsageLogService` 생성자 주입 추가.
새 엔드포인트:
```java
@GetMapping("/ai-usage")
public ApiResponse<AiUsageResponse> aiUsage(
    @RequestParam(defaultValue = "DAILY") String granularity,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    Authentication authentication
) {
    return ApiResponse.success(
        aiUsageLogService.getAiUsage(
            currentUserId(authentication),
            AiUsageGranularity.from(granularity),
            startDate,
            endDate
        )
    );
}
```

**요청 예시:**
- `GET /api/v1/analytics/ai-usage` → DAILY 기본값 (최근 7일)
- `GET /api/v1/analytics/ai-usage?granularity=WEEKLY` → WEEKLY 기본값 (최근 8주)
- `GET /api/v1/analytics/ai-usage?granularity=DAILY&startDate=2026-04-01&endDate=2026-04-30` → 4월 전체, 일별 집계
- `GET /api/v1/analytics/ai-usage?granularity=MONTHLY&startDate=2025-06-01&endDate=2026-05-15` → 약 1년, 월별 집계

---

## Phase 2 — Flutter

### 새 파일

#### `ai_usage_entity.dart` (Domain Entity)
```dart
enum AiUsageGranularity { daily, weekly, monthly }

class AiUsagePeriodPoint { final String label; final int inputTokens, outputTokens, sessions; }
class AiUsageModelShare { final String model; final int totalTokens, sessions; }
class AiUsageEntity {
  final AiUsageGranularity granularity;
  final DateTime startDate;
  final DateTime endDate;
  final int totalInputTokens, totalOutputTokens, totalSessions;
  final String? mostUsedModel;
  final List<AiUsagePeriodPoint> trend;
  final List<AiUsageModelShare> modelDistribution;
  int get totalTokens => totalInputTokens + totalOutputTokens;
  double get avgTokensPerSession => totalSessions > 0 ? totalTokens / totalSessions : 0.0;
}
```

#### `ai_usage_model.dart` (Data Model)
JSON → Entity 변환. Spring record 필드명(camelCase) 그대로 파싱.

#### 위젯 (3개 신규)

**`ai_usage_summary_card.dart`**
- `GlassCard` 내부
- 3-stat row: 총 입력 / 총 출력 / 세션 수 (숫자 abbreviated: 1.2K, 300K 등)
- 아래: 주요 모델명 + 세션당 평균 토큰

**`token_trend_chart.dart`**
- `GlassCard` + `LineChart` (fl_chart)
- 2개 라인: 입력(AppColors.violet) / 출력(AppColors.info)
- X축: trend의 label 문자열 (DAILY: 05/15, WEEKLY: W20, MONTHLY: 5월)
- Y축: 축약 숫자 (12K, 1.2M)
- 터치 툴팁: label + 입력/출력 각각 표시
- 데이터 없을 시 "데이터가 없습니다" 고정 높이(200) 빈 상태

**`model_distribution_chart.dart`**
- `GlassCard` + `PieChart` (donut, `SourceDistributionChart` 패턴 재사용)
- 색상: `[primary, info, success, warning, error]` 순환
- 범례: 모델명(20자 truncate) + "X,XXX tok"
- 단일 모델이면 100% 풀 원형

---

### 수정 파일

#### `analytics_remote_datasource.dart`
```dart
Future<AiUsageModel> fetchAiUsage(AiUsageFilter filter) async {
  final response = await _dio.get('/api/v1/analytics/ai-usage',
    queryParameters: {
      'granularity': filter.granularity.name.toUpperCase(),
      'startDate': _formatDate(filter.startDate),   // "2026-04-01"
      'endDate':   _formatDate(filter.endDate),
    });
  if (response.data['success'] == true) return AiUsageModel.fromJson(response.data['data']);
  throw Exception(response.data['error']['message']);
}
```

#### `analytics_repository.dart` (interface) + `analytics_repository_impl.dart`
`fetchAiUsage(AiUsageFilter filter)` 메서드 추가.

#### `analytics_providers.dart`

```dart
// 날짜 범위 + granularity를 담는 필터 값 객체
class AiUsageFilter {
  final AiUsageGranularity granularity;
  final DateTime startDate;
  final DateTime endDate;
  const AiUsageFilter({required this.granularity, required this.startDate, required this.endDate});

  // granularity 변경 시 기본 범위로 리셋
  static AiUsageFilter defaultFor(AiUsageGranularity g) {
    final now = DateTime.now();
    return switch (g) {
      AiUsageGranularity.daily   => AiUsageFilter(g, now.subtract(const Duration(days: 6)), now),
      AiUsageGranularity.weekly  => AiUsageFilter(g, now.subtract(const Duration(days: 55)), now),
      AiUsageGranularity.monthly => AiUsageFilter(g, DateTime(now.year - 1, now.month, now.day), now),
    };
  }
}

final aiUsageFilterProvider = StateProvider<AiUsageFilter>(
  (ref) => AiUsageFilter.defaultFor(AiUsageGranularity.daily),
);

final aiUsageProvider = FutureProvider.family<AiUsageEntity, AiUsageFilter>((ref, filter) async {
  return ref.watch(analyticsRepositoryProvider).fetchAiUsage(filter);
});
```

refresh 로직 (기존 `refreshAnalyticsProvider` 확장):
```dart
ref.invalidate(analyticsRepositoryProvider);
ref.invalidate(weeklyAnalyticsProvider);
ref.invalidate(aiUsageProvider(ref.read(aiUsageFilterProvider)));
```

#### `analytics_screen.dart`
`ConsumerWidget` → `ConsumerStatefulWidget` 전환.
`DefaultTabController(length: 2)` 래핑:
- TabBar: `알림 통계` | `AI 토큰`
- TabBarView:
  - Tab 0: 기존 내용을 `_NotificationAnalyticsTab`으로 추출
  - Tab 1: `_AiUsageTab` (신규)

`_AiUsageTab` 구조:
```
Column
├── _GranularitySelectorRow (일별 | 주별 | 월별 ChoiceChip)
│   → 변경 시 ref.read(aiUsageFilterProvider.notifier).state = AiUsageFilter.defaultFor(newGranularity)
├── _DateRangeSelector
│   → 시작일/종료일 텍스트 버튼 → showDateRangePicker()
│   → 선택 후 ref.read(aiUsageFilterProvider.notifier).state = AiUsageFilter(granularity, start, end)
└── Expanded
    └── aiUsageProvider(filter).when(
        data: SingleChildScrollView → AiUsageSummaryCard, TokenTrendChart, ModelDistributionChart
        loading: CircularProgressIndicator
        error: 에러 + 재시도 버튼
    )
```

`_DateRangeSelector` 디자인:
- Row: [캘린더 아이콘] [시작일 버튼] ~ [종료일 버튼]
- 각 버튼 탭 → Flutter 내장 `showDateRangePicker()` 실행
- 날짜 포맷: "2026.04.01" 형태
- granularity 칩 변경 시 날짜 범위 자동 리셋 (defaultFor 호출)

---

## 파일 목록 요약

### 신규 (Backend)
- `db/migration/V15__create_ai_usage_logs.sql`
- `com.notio.analytics.domain.AiUsageLog`
- `com.notio.analytics.repository.AiUsageLogRepository`
- `com.notio.analytics.repository.AiUsageDataPoint` (projection interface)
- `com.notio.analytics.repository.ModelUsageDataPoint` (projection interface)
- `com.notio.analytics.service.AiUsageLogService`
- `com.notio.analytics.dto.AiUsageGranularity`
- `com.notio.analytics.dto.AiUsageResponse`

### 수정 (Backend)
- `com.notio.notification.service.NotificationService` — Branch D 추가
- `com.notio.analytics.controller.AnalyticsController` — `/ai-usage` 엔드포인트 추가

### 신규 (Flutter)
- `features/analytics/domain/entity/ai_usage_entity.dart`
- `features/analytics/data/model/ai_usage_model.dart`
- `features/analytics/presentation/widgets/ai_usage_summary_card.dart`
- `features/analytics/presentation/widgets/token_trend_chart.dart`
- `features/analytics/presentation/widgets/model_distribution_chart.dart`

### 수정 (Flutter)
- `features/analytics/data/datasource/analytics_remote_datasource.dart`
- `features/analytics/domain/repository/analytics_repository.dart`
- `features/analytics/data/repository/analytics_repository_impl.dart`
- `features/analytics/presentation/providers/analytics_providers.dart`
- `features/analytics/presentation/analytics_screen.dart`

---

## 검증

### Backend
1. Flyway V15 적용 확인: `SELECT * FROM flyway_schema_history WHERE version = '15'`
2. 테스트 webhook POST → `ai_usage_logs` 행 생성 확인 + `event=ai_usage_log_created` 로그
3. 토큰 0 페이로드 → 행 미생성 확인 + `event=ai_usage_log_skip reason=zero_tokens`
4. 동일 notification_id 중복 → 1행만 존재 확인
5. `GET /api/v1/analytics/ai-usage` (파라미터 없음) → 기본 범위 응답
6. `GET /api/v1/analytics/ai-usage?granularity=DAILY&startDate=2026-04-01&endDate=2026-04-30` → 30개 이하 포인트, label YYYY-MM-DD 포맷
7. `GET /api/v1/analytics/ai-usage?granularity=DAILY&startDate=2026-01-01&endDate=2026-12-31` → 91일 초과 → 400 + INVALID_REQUEST
8. `startDate > endDate` → 400 + INVALID_REQUEST
9. `granularity=INVALID` → 400 + INVALID_REQUEST
10. 단위 테스트: `AiUsageLogServiceTest` (로깅 5개 케이스 + 날짜 범위 검증 3개 케이스)
11. 슬라이스 테스트: `@WebMvcTest(AnalyticsController)` 신규 엔드포인트

### Frontend
1. `flutter analyze` 경고 0개
2. 탭 전환 → 각 탭 스크롤 위치 독립 유지
3. Granularity 칩 변경 → 날짜 범위 자동 리셋 → 데이터 재조회 확인
4. 날짜 범위 picker → 시작/종료일 선택 → 선택된 날짜 표시 + 데이터 재조회 확인
5. granularity=DAILY에서 91일 이상 선택 시 → 서버 400 응답 → 에러 상태 렌더링
6. 빈 데이터 → 각 차트 빈 상태 렌더링 (에러 없음)
7. 새로고침 → 양 탭 데이터 갱신 확인 (현재 선택된 필터 유지)
