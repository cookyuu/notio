# Notio — Frontend 개발 계획서 (plan_frontend.md)

> Flutter 3.x · Dart 3.x · MVP (Phase 0)

---

## 1. 기술 스택

| 분류 | 패키지 | 버전 |
|------|--------|------|
| 언어 | Dart | 3.6.x |
| 프레임워크 | Flutter | 3.x |
| 상태관리 | hooks_riverpod + flutter_hooks | 2.5.3 / 0.20.5 |
| 코드 생성 | riverpod_generator + build_runner | 2.6.2 / 2.4.13 |
| 네비게이션 | go_router | 14.6.1 |
| 네트워크 | dio + retrofit | 5.4.3 / 4.1.0 |
| JSON | json_annotation + json_serializable | 4.9.0 / 6.8.0 |
| 로컬 DB | drift + drift_dev | 2.18.0 |
| SQLite | sqlite3_flutter_libs + path_provider | 0.5.24 / 2.1.4 |
| 푸시 알림 | firebase_core + firebase_messaging | 2.27.2 / 14.7.20 |
| 로컬 알림 | flutter_local_notifications | 17.2.3 |
| 리스트 제스처 | flutter_slidable | 3.1.1 |
| 차트 | fl_chart | 0.68.0 |
| 보안 스토리지 | flutter_secure_storage | 9.2.2 |
| 프리퍼런스 | shared_preferences | 2.3.3 |
| 시간 포맷 | timeago | 3.6.1 |
| 국제화 | intl | 0.19.0 |
| 유틸리티 | freezed_annotation + freezed | 2.4.4 / 2.5.7 |
| Lint | flutter_lints + custom_lint + riverpod_lint | 5.0.0 / 0.6.7 / 2.5.1 |

---

## 2. 폴더 구조

```
frontend/
├── pubspec.yaml
├── analysis_options.yaml
├── Makefile                         # build_runner, gen 단축 명령
├── firebase.json / google-services.json
└── lib/
    ├── main.dart
    │
    ├── core/
    │   ├── di/                      # Riverpod provider 등록
    │   │   └── providers.dart
    │   ├── network/
    │   │   ├── dio_client.dart      # Dio 인스턴스 + interceptor
    │   │   ├── auth_interceptor.dart
    │   │   └── api_response.dart    # 공통 응답 모델
    │   ├── router/
    │   │   └── app_router.dart      # go_router 정의
    │   ├── storage/
    │   │   └── secure_storage.dart  # JWT 토큰 관리
    │   ├── theme/
    │   │   ├── app_theme.dart       # ThemeData 정의
    │   │   ├── app_colors.dart      # 컬러 토큰
    │   │   ├── app_text_styles.dart
    │   │   └── app_spacing.dart     # 간격 상수
    │   └── shell/
    │       └── main_shell.dart      # 하단 탭 ShellRoute
    │
    ├── features/
    │   │
    │   ├── notification/            # 알림 탭
    │   │   ├── data/
    │   │   │   ├── datasource/
    │   │   │   │   ├── notification_remote_datasource.dart
    │   │   │   │   └── notification_local_datasource.dart  # Drift
    │   │   │   ├── model/
    │   │   │   │   └── notification_model.dart             # JSON 직렬화
    │   │   │   └── repository/
    │   │   │       └── notification_repository_impl.dart
    │   │   ├── domain/
    │   │   │   ├── entity/
    │   │   │   │   ├── notification_entity.dart
    │   │   │   │   ├── notification_source.dart            # enum
    │   │   │   │   └── notification_priority.dart          # enum
    │   │   │   └── repository/
    │   │   │       └── notification_repository.dart        # abstract
    │   │   └── presentation/
    │   │       ├── provider/
    │   │       │   ├── notifications_provider.dart
    │   │       │   └── unread_count_provider.dart
    │   │       ├── screen/
    │   │       │   └── notifications_screen.dart
    │   │       └── widget/
    │   │           ├── notification_card.dart
    │   │           ├── source_filter_chips.dart
    │   │           ├── ai_summary_card.dart
    │   │           └── notification_detail_sheet.dart
    │   │
    │   ├── chat/                    # AI 채팅 탭
    │   │   ├── data/
    │   │   │   ├── datasource/
    │   │   │   │   └── chat_remote_datasource.dart
    │   │   │   ├── model/
    │   │   │   │   ├── chat_message_model.dart
    │   │   │   │   └── daily_summary_model.dart
    │   │   │   └── repository/
    │   │   │       └── chat_repository_impl.dart
    │   │   ├── domain/
    │   │   │   ├── entity/
    │   │   │   │   ├── chat_message_entity.dart
    │   │   │   │   └── message_role.dart                   # enum
    │   │   │   └── repository/
    │   │   │       └── chat_repository.dart
    │   │   └── presentation/
    │   │       ├── provider/
    │   │       │   ├── chat_provider.dart
    │   │       │   ├── daily_summary_provider.dart
    │   │       │   └── pending_todo_context_provider.dart
    │   │       ├── screen/
    │   │       │   └── chat_screen.dart
    │   │       └── widget/
    │   │           ├── chat_bubble.dart
    │   │           ├── quick_chip_row.dart
    │   │           ├── todo_context_card.dart              # 알림→할일 컨텍스트
    │   │           ├── todo_result_card.dart
    │   │           └── typing_indicator.dart
    │   │
    │   ├── analytics/               # 분석 탭
    │   │   ├── data/
    │   │   │   ├── datasource/
    │   │   │   │   └── analytics_remote_datasource.dart
    │   │   │   ├── model/
    │   │   │   │   └── weekly_analytics_model.dart
    │   │   │   └── repository/
    │   │   │       └── analytics_repository_impl.dart
    │   │   ├── domain/
    │   │   │   └── repository/
    │   │   │       └── analytics_repository.dart
    │   │   └── presentation/
    │   │       ├── provider/
    │   │       │   └── analytics_provider.dart
    │   │       ├── screen/
    │   │       │   └── analytics_screen.dart
    │   │       └── widget/
    │   │           ├── weekly_bar_chart.dart
    │   │           ├── source_donut_chart.dart
    │   │           ├── stat_card.dart
    │   │           └── llm_report_card.dart
    │   │
    │   └── settings/                # 설정 탭
    │       └── presentation/
    │           ├── provider/
    │           │   └── settings_provider.dart
    │           ├── screen/
    │           │   └── settings_screen.dart
    │           └── widget/
    │               ├── source_toggle_item.dart
    │               └── server_url_row.dart
    │
    └── shared/
        ├── widget/
        │   ├── glass_card.dart          # 글래스모피즘 카드 공통 위젯
        │   ├── notio_app_bar.dart
        │   ├── source_badge.dart        # 소스별 색상 뱃지
        │   ├── empty_state.dart
        │   └── loading_overlay.dart
        ├── extension/
        │   ├── datetime_extension.dart
        │   └── string_extension.dart
        └── constant/
            └── api_constants.dart       # base URL, endpoint 상수
```

---

## 3. 네이밍 규칙

### 파일명
- 모두 `snake_case`: `notification_card.dart`, `app_colors.dart`
- 화면: `{feature}_screen.dart`
- 위젯: `{기능}_widget.dart` 또는 `{기능}.dart`
- Provider: `{feature}_provider.dart`
- Repository (abstract): `{feature}_repository.dart`
- Repository (구현체): `{feature}_repository_impl.dart`

### 클래스명
| 유형 | 규칙 | 예시 |
|------|------|------|
| 화면 Widget | `{Feature}Screen` | `NotificationsScreen` |
| 일반 Widget | `{기능}Widget` 또는 `{기능}` | `NotificationCard`, `GlassCard` |
| Entity | `{Domain}Entity` | `NotificationEntity` |
| Model (DTO) | `{Domain}Model` | `NotificationModel` |
| Provider | `{feature}Provider` (camelCase) | `notificationsProvider` |
| Notifier | `{Feature}Notifier` | `NotificationsNotifier` |
| Repository (abstract) | `{Feature}Repository` | `NotificationRepository` |
| Repository (impl) | `{Feature}RepositoryImpl` | `NotificationRepositoryImpl` |
| Enum | `PascalCase` | `NotificationSource`, `MessageRole` |
| Enum 값 | `camelCase` (Dart 관례) | `claude`, `inProgress` |
| Extension | `{Type}Extension` | `DateTimeExtension` |

### Provider 네이밍
```dart
// 목록: 복수형
final notificationsProvider = ...
final chatMessagesProvider  = ...

// 단건 / 상태: 명사 + 상태 표현
final unreadCountProvider       = ...
final selectedSourceProvider    = ...
final pendingTodoContextProvider = ...
final dailySummaryProvider      = ...
```

### 변수
- 일반: `camelCase`
- 상수: `lowerCamelCase` (Dart 관례) — `const baseUrl = '...'`
- private: `_camelCase` 접두사
- Boolean: `is`, `has`, `can` 접두사 (`isRead`, `hasContext`)

### 라우트 경로
```dart
// go_router path 상수 — ApiConstants 또는 AppRoutes 클래스에 정의
static const notifications = '/notifications';
static const chat          = '/chat';
static const analytics     = '/analytics';
static const settings      = '/settings';
```

---

## 4. 디자인 시스템 토큰 (app_colors.dart)

```dart
// 배경
static const bg0 = Color(0xFF0A0A12);  // 최하단 배경
static const bg1 = Color(0xFF0F0F1A);  // 앱 기본 배경
static const bg2 = Color(0xFF141428);  // 카드 배경
static const bg3 = Color(0xFF1C1C35);  // 바텀시트

// 액센트 (바이올렛)
static const violet     = Color(0xFF7C5CFC);
static const violet2    = Color(0xFF9B7DFF);
static const violet3    = Color(0xFFC4B0FF);
static const violetGlow = Color(0x4D7C5CFC);  // 30% opacity
static const violetSoft = Color(0x1F7C5CFC);  // 12% opacity

// 텍스트
static const text1 = Color(0xFFF0EEFF);  // 주요 텍스트
static const text2 = Color(0xFFA09DC0);  // 보조 텍스트
static const text3 = Color(0xFF5C5980);  // 힌트 텍스트

// 글래스
static const glass  = Color(0x14FFFFFF);  // 8% white
static const glass2 = Color(0x1FFFFFFF);  // 12% white
static const border = Color(0x17FFFFFF);  // 9% white
static const border2= Color(0x26FFFFFF);  // 15% white

// 소스별 색상
static const srcClaude = Color(0xFFA78BFA);
static const srcSlack  = Color(0xFFFB923C);
static const srcGithub = Color(0xFF94A3B8);
static const srcGmail  = Color(0xFFF87171);
static const srcInt    = Color(0xFF34D399);  // 내부 앱
```

### Typography (app_text_styles.dart)

```dart
static const h1 = TextStyle(
  fontSize: 28,
  fontWeight: FontWeight.w700,
  color: AppColors.text1,
);

static const h2 = TextStyle(
  fontSize: 22,
  fontWeight: FontWeight.w600,
  color: AppColors.text1,
);

static const body1 = TextStyle(
  fontSize: 16,
  fontWeight: FontWeight.w400,
  color: AppColors.text1,
);

static const body2 = TextStyle(
  fontSize: 14,
  fontWeight: FontWeight.w400,
  color: AppColors.text2,
);

static const caption = TextStyle(
  fontSize: 12,
  fontWeight: FontWeight.w400,
  color: AppColors.text3,
);
```

### Spacing (app_spacing.dart)

```dart
static const s4 = 4.0;
static const s8 = 8.0;
static const s12 = 12.0;
static const s16 = 16.0;
static const s20 = 20.0;
static const s24 = 24.0;
static const s32 = 32.0;
```

---

## 5. GlassCard 컴포넌트 규칙

글래스모피즘 카드는 `shared/widget/glass_card.dart`에서 공통으로 사용한다.

```
GlassCard(
  child: ...,
  padding: EdgeInsets.all(12),   // 기본 패딩
  borderRadius: 14,               // 기본 radius
  accentColor: null,              // 왼쪽 액센트 라인 (선택)
)
```

- 배경: `AppColors.glass` (`rgba(255,255,255,0.08)`)
- 테두리: `AppColors.border2` 0.5px
- 액센트 라인 있을 때: 왼쪽 3px 바이올렛 라인

### GlassCard 구현 예시

```dart
class GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsets padding;
  final double borderRadius;
  final Color? accentColor;  // 왼쪽 액센트 라인

  const GlassCard({
    required this.child,
    this.padding = const EdgeInsets.all(12),
    this.borderRadius = 14,
    this.accentColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: AppColors.glass,
        borderRadius: BorderRadius.circular(borderRadius),
        border: Border.all(color: AppColors.border2, width: 0.5),
        // 왼쪽 액센트 라인 (선택적)
      ),
      child: child,
    );
  }
}
```

---

## 6. SourceBadge 컴포넌트

소스별 색상 뱃지는 `shared/widget/source_badge.dart`에서 구현합니다.

```dart
class SourceBadge extends StatelessWidget {
  final NotificationSource source;

  Color _getColor() {
    switch (source) {
      case NotificationSource.claude: return AppColors.srcClaude;
      case NotificationSource.slack: return AppColors.srcSlack;
      case NotificationSource.github: return AppColors.srcGithub;
      case NotificationSource.gmail: return AppColors.srcGmail;
      case NotificationSource.internal: return AppColors.srcInt;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: _getColor().withOpacity(0.15),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        source.name.toUpperCase(),
        style: TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.w600,
          color: _getColor(),
        ),
      ),
    );
  }
}
```

---

## 7. 라우팅 구조 (go_router)

```
/ (ShellRoute — MainShell)
├── /notifications          # 알림 목록
├── /chat                   # AI 채팅
├── /analytics              # 분석
└── /settings               # 설정
```

- 딥링크: `notio://notifications`, `notio://chat`
- 알림 탭에서 채팅 탭으로 컨텍스트 전달: `pendingTodoContextProvider` 상태로 처리 (쿼리 파라미터 사용 금지)

---

## 8. 오프라인 캐시 전략 (Drift)

| 데이터 | 캐시 정책 | TTL |
|--------|-----------|-----|
| 알림 목록 | 항상 로컬 저장, 백그라운드 갱신 | 무제한 (수동 삭제) |
| 오늘 요약 | FutureProvider 메모리 캐시 | 앱 생명주기 내 |
| 채팅 이력 | 로컬 저장 | 무제한 |

---

## 9. SSE 스트리밍 구현 규칙

- `dio` `ResponseType.stream` 사용
- 청크 수신 시 `chatMessagesProvider`에 append
- 연결 끊김 감지 → Exponential Backoff (1s → 2s → 4s, 최대 3회) 재연결
- `AppLifecycleState.resumed` 시 미완료 스트림 재연결
- 짧은 응답(할일 생성, 검색): SSE 아닌 POST 단건 사용

---

## 10. MVP 체크리스트

### 환경 세팅
- [ ] Flutter 프로젝트 생성 (`flutter create --org com.notio notio_app`)
- [ ] `pubspec.yaml` 패키지 설정
- [ ] `analysis_options.yaml` lint 규칙 설정
- [ ] 폴더 구조 생성 (feature-first)
- [ ] `app_colors.dart` — 디자인 토큰 정의
- [ ] `app_theme.dart` — `ThemeData.dark()` 기반 커스텀 테마
- [ ] `app_text_styles.dart` — 텍스트 스타일 상수
- [ ] `GlassCard` 공통 위젯 구현
- [ ] `go_router` 설정 + `MainShell` (하단 탭 4개)
- [ ] `DioClient` + `AuthInterceptor` 설정
- [ ] Firebase 프로젝트 연동 (`google-services.json` / `GoogleService-Info.plist`)
- [ ] GitHub Actions `ci-frontend.yml` — `frontend/**` 변경 시만 실행

### 알림 탭
- [ ] `NotificationEntity` 도메인 모델 정의
- [ ] `NotificationSource` / `NotificationPriority` enum 정의
- [ ] `NotificationRemoteDatasource` — API 호출
- [ ] `NotificationLocalDatasource` — Drift 캐시
- [ ] `NotificationRepositoryImpl` 구현
- [ ] `notificationsProvider` (AsyncNotifier)
- [ ] `unreadCountProvider` (StreamProvider)
- [ ] `selectedSourceProvider` (StateProvider)
- [ ] `NotificationsScreen` 구현
  - [ ] AppBar (Notio 로고, 미읽음 뱃지)
  - [ ] `SourceFilterChips` 위젯 (전체/Claude/Slack/GitHub/Gmail)
  - [ ] `AiSummaryCard` 위젯 (오늘 요약 카드)
  - [ ] 알림 목록 ListView (무한 스크롤)
  - [ ] `NotificationCard` 위젯
    - [ ] 미읽음 도트 (바이올렛 glow)
    - [ ] `SourceBadge` 위젯 (소스별 색상)
    - [ ] 제목 + 시간 + 우선순위 뱃지
  - [ ] 스와이프 → "할일 생성" 액션 (`flutter_slidable`)
  - [ ] 당겨서 새로고침
- [ ] `NotificationDetailSheet` 바텀시트
  - [ ] 전체 본문 표시
  - [ ] "닫기" / "할일 생성" 버튼
  - [ ] "할일 생성" → `pendingTodoContextProvider` 설정 → `/chat` 이동
- [ ] Empty State — 소스 미연결 온보딩 카드

### AI 채팅 탭
- [ ] `ChatMessageEntity` / `MessageRole` enum 정의
- [ ] `ChatRemoteDatasource` — POST /chat, SSE /chat/stream
- [ ] `ChatRepositoryImpl` 구현
- [ ] `chatMessagesProvider` (StateNotifier)
- [ ] `dailySummaryProvider` (FutureProvider)
- [ ] `pendingTodoContextProvider` (StateProvider\<NotificationEntity?\>)
- [ ] `ChatScreen` 구현
  - [ ] 상단 상태 표시 (llama3.2 · RAG 활성)
  - [ ] `QuickChipRow` 위젯 (오늘 요약 / 중요한 거 / 패턴)
    - [ ] 알림 없을 때 비활성화
  - [ ] `TodoContextCard` 위젯 (pendingTodoContext 있을 때 표시)
  - [ ] `ChatBubble` 위젯 (user / ai 구분)
    - [ ] user: 바이올렛 배경
    - [ ] ai: 글래스 카드
  - [ ] AI 응답 내 `TodoResultCard` 위젯
  - [ ] `TypingIndicator` 애니메이션
  - [ ] SSE 스트리밍 수신 + append
  - [ ] 재연결 로직 (Exponential Backoff)
  - [ ] 채팅 입력창 + 전송 버튼 (바이올렛 glow)
- [ ] Empty State — 소스 미연결 안내

### 분석 탭
- [ ] `WeeklyAnalyticsModel` 정의
- [ ] `analyticsProvider` (FutureProvider)
- [ ] `AnalyticsScreen` 구현
  - [ ] 통계 카드 그리드 (총 알림, 할일 생성, 미읽음, 응답률)
  - [ ] `WeeklyBarChart` — 일별 알림 수신량 (fl_chart)
  - [ ] `SourceDonutChart` — 소스별 분포 (fl_chart)
  - [ ] `LlmReportCard` — AI 주간 리포트 (바이올렛 액센트 라인)

### 설정 탭
- [ ] `SettingsScreen` 구현
  - [ ] 소스 연결 토글 목록 (Claude / Slack / GitHub / Gmail / 내부 앱)
  - [ ] Ollama 서버 URL 표시 + 연결 상태 뱃지
  - [ ] LLM 모델 표시
  - [ ] Claude API Fallback 토글

### 푸시 알림
- [ ] `FirebaseMessaging` 초기화 (`main.dart`)
- [ ] 백그라운드 핸들러 등록 (`@pragma('vm:entry-point')`)
- [ ] FCM 토큰 발급 + `POST /api/v1/devices/register` 호출
- [ ] 포그라운드 푸시 수신 → 알림 목록 자동 갱신
- [ ] 백그라운드 / 종료 상태 푸시 탭 → 해당 알림 상세로 딥링크
- [ ] `flutter_local_notifications` — 포그라운드 알림 표시

### 로컬 캐시 (Drift)
- [ ] Drift 데이터베이스 클래스 정의 (`AppDatabase`)
- [ ] `notifications` 테이블 정의
- [ ] `chat_messages` 테이블 정의
- [ ] 오프라인 시 로컬 데이터 우선 표시
- [ ] 네트워크 복구 시 자동 갱신

### 코드 품질
- [ ] `flutter analyze` 경고 0개 유지
- [ ] `flutter_lints` + 커스텀 lint 규칙 적용
- [ ] 주요 Provider 단위 테스트 작성
- [ ] 골든 테스트 — `NotificationCard`, `GlassCard`, `SourceBadge`
