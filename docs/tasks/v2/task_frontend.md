# Task: AI Token Usage Analytics — Flutter 구현 체크리스트

> **대상 버전**: v2.3
> **작성일**: 2026-05-15
> **연관 Plan**: `docs/plans/v2/plan_fix.md`

---

## Phase 1: 신규 파일 — Domain Entity

- [ ] `features/analytics/domain/entity/ai_usage_entity.dart` 생성
  - [ ] `AiUsageGranularity` enum 정의 (`daily`, `weekly`, `monthly`)
  - [ ] `AiUsagePeriodPoint` 클래스 정의 (`label`, `inputTokens`, `outputTokens`, `sessions`)
  - [ ] `AiUsageModelShare` 클래스 정의 (`model`, `totalTokens`, `sessions`)
  - [ ] `AiUsageEntity` 클래스 정의
    - [ ] 필드: `granularity`, `startDate`, `endDate`, `totalInputTokens`, `totalOutputTokens`, `totalSessions`, `mostUsedModel`(nullable), `trend`, `modelDistribution`
    - [ ] `int get totalTokens` computed getter
    - [ ] `double get avgTokensPerSession` computed getter (totalSessions == 0 → 0.0)

---

## Phase 2: 신규 파일 — Data Model

- [ ] `features/analytics/data/model/ai_usage_model.dart` 생성
  - [ ] `AiUsageModel` 클래스 + `fromJson` 팩토리 메서드 구현
  - [ ] Spring record 필드명(camelCase) 그대로 파싱
  - [ ] `toEntity()` 메서드로 `AiUsageEntity` 변환

---

## Phase 3: 신규 파일 — Widgets

### AiUsageSummaryCard

- [ ] `features/analytics/presentation/widgets/ai_usage_summary_card.dart` 생성
  - [ ] `GlassCard` 래핑
  - [ ] 3-stat row: 총 입력 / 총 출력 / 세션 수 (숫자 abbreviated: 1.2K, 300K 등)
  - [ ] 아래: 주요 모델명 + 세션당 평균 토큰 표시

### TokenTrendChart

- [ ] `features/analytics/presentation/widgets/token_trend_chart.dart` 생성
  - [ ] `GlassCard` + `LineChart` (fl_chart) 래핑
  - [ ] 입력 라인: `AppColors.violet`, 출력 라인: `AppColors.info`
  - [ ] X축: trend label 문자열 (DAILY: `05/15`, WEEKLY: `W20`, MONTHLY: `5월`)
  - [ ] Y축: 축약 숫자 (`12K`, `1.2M`)
  - [ ] 터치 툴팁: label + 입력/출력 각각 표시
  - [ ] 빈 데이터 상태: "데이터가 없습니다" 고정 높이(200) 렌더링

### ModelDistributionChart

- [ ] `features/analytics/presentation/widgets/model_distribution_chart.dart` 생성
  - [ ] `GlassCard` + `PieChart` (donut, `SourceDistributionChart` 패턴 참고)
  - [ ] 색상: `[primary, info, success, warning, error]` 순환
  - [ ] 범례: 모델명(20자 truncate) + `"X,XXX tok"` 표시
  - [ ] 단일 모델: 100% 풀 원형 렌더링

---

## Phase 4: 기존 파일 수정 — Data Layer

### analytics_remote_datasource.dart

- [ ] `AiUsageFilter` import 추가
- [ ] `fetchAiUsage(AiUsageFilter filter)` 메서드 추가
  - [ ] `GET /api/v1/analytics/ai-usage` 호출
  - [ ] query params: `granularity`(toUpperCase), `startDate`("yyyy-MM-dd"), `endDate`("yyyy-MM-dd")
  - [ ] `response.data['success'] == true` → `AiUsageModel.fromJson(response.data['data'])` 반환
  - [ ] 실패 시 `Exception(response.data['error']['message'])` throw

### analytics_repository.dart (interface)

- [ ] `fetchAiUsage(AiUsageFilter filter)` 추상 메서드 추가

### analytics_repository_impl.dart

- [ ] `fetchAiUsage(AiUsageFilter filter)` 구현 (datasource 위임)

---

## Phase 5: 기존 파일 수정 — Providers

### analytics_providers.dart

- [ ] `AiUsageFilter` 값 객체 정의
  - [ ] 필드: `granularity`, `startDate`, `endDate`
  - [ ] `defaultFor(AiUsageGranularity g)` static 팩토리 메서드
    - [ ] DAILY → 최근 7일 (now - 6일 ~ now)
    - [ ] WEEKLY → 최근 8주 (now - 55일 ~ now)
    - [ ] MONTHLY → 최근 12개월 (1년 전 ~ now)
- [ ] `aiUsageFilterProvider` `StateProvider<AiUsageFilter>` 추가 (초기값: DAILY 기본)
- [ ] `aiUsageProvider` `FutureProvider.family<AiUsageEntity, AiUsageFilter>` 추가
- [ ] 기존 refresh 로직에 `ref.invalidate(aiUsageProvider(ref.read(aiUsageFilterProvider)))` 추가

---

## Phase 6: 기존 파일 수정 — analytics_screen.dart

- [ ] `ConsumerWidget` → `ConsumerStatefulWidget` 전환
- [ ] `DefaultTabController(length: 2)` 래핑
- [ ] `TabBar` 추가: `알림 통계` | `AI 토큰`
- [ ] `TabBarView` 구성
  - [ ] Tab 0: 기존 Analytics 내용을 `_NotificationAnalyticsTab` 위젯으로 추출
  - [ ] Tab 1: `_AiUsageTab` 신규 구현
- [ ] `_GranularitySelectorRow` 위젯 구현
  - [ ] `일별` | `주별` | `월별` ChoiceChip 렌더링
  - [ ] 변경 시 `AiUsageFilter.defaultFor(newGranularity)`로 provider state 업데이트
- [ ] `_DateRangeSelector` 위젯 구현
  - [ ] Row: [캘린더 아이콘] [시작일 버튼] ~ [종료일 버튼]
  - [ ] 탭 → `showDateRangePicker()` 실행
  - [ ] 날짜 포맷: `"2026.04.01"` 형태
  - [ ] granularity 칩 변경 시 날짜 범위 자동 리셋
- [ ] `_AiUsageTab` 본문 구현
  - [ ] `aiUsageProvider(filter).when` 처리
    - [ ] `data`: `SingleChildScrollView` → `AiUsageSummaryCard`, `TokenTrendChart`, `ModelDistributionChart`
    - [ ] `loading`: `CircularProgressIndicator`
    - [ ] `error`: 에러 메시지 + 재시도 버튼

---

## 최종 검증

- [ ] `flutter analyze` 경고 0개
- [ ] 탭 전환 → 각 탭 스크롤 위치 독립 유지 확인
- [ ] Granularity 칩 변경 → 날짜 범위 자동 리셋 → 데이터 재조회 확인
- [ ] 날짜 범위 picker → 시작/종료일 선택 → 선택된 날짜 표시 + 데이터 재조회 확인
- [ ] DAILY에서 91일 이상 선택 → 서버 400 응답 → 에러 상태 렌더링 확인
- [ ] 빈 데이터 → 각 차트 빈 상태 렌더링 (에러 없음) 확인
- [ ] 새로고침 → 양 탭 데이터 갱신 + 현재 필터 유지 확인
