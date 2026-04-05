# Phase 0: 환경 세팅 및 기본 인프라 ✅

완료일: 2026-04-05

## 완료 항목

### 1. 프로젝트 초기화 ✅
- Flutter 3.x 프로젝트 생성 완료
- Dart 3.6.x 설정 완료

### 2. 의존성 설정 ✅
모든 필수 패키지 추가 완료:
- **상태관리**: `hooks_riverpod: ^2.5.3`, `flutter_hooks: ^0.20.5`
- **코드 생성**: `riverpod_generator: ^2.6.2`, `build_runner: ^2.4.13`
- **네비게이션**: `go_router: ^14.6.1`
- **네트워크**: `dio: ^5.4.3`, `retrofit: ^4.1.0`
- **로컬 DB**: `drift: ^2.18.0`
- **푸시 알림**: `firebase_core: ^2.27.2`, `firebase_messaging: ^14.7.20`
- **UI 컴포넌트**: `flutter_slidable: ^3.1.1`, `fl_chart: ^0.68.0`
- **보안 저장소**: `flutter_secure_storage: ^9.2.2`
- **기타**: `timeago: ^3.6.1`, `intl: ^0.19.0`

### 3. 코드 품질 설정 ✅
`analysis_options.yaml` 설정 완료:
- `flutter_lints: ^5.0.0` 활성화
- `custom_lint: ^0.7.0` 추가
- `riverpod_lint: ^2.6.0` 추가
- 커스텀 린트 규칙 적용

### 4. Feature-first 폴더 구조 ✅
```
lib/
├── core/
│   ├── constants/      # 상수 (AppSpacing, NotificationSource)
│   ├── theme/          # 테마 (AppColors, AppTextStyles, AppTheme)
│   ├── router/         # 라우팅 (AppRouter, Routes)
│   ├── network/        # 네트워크 (DioClient, Interceptors)
│   └── database/       # 데이터베이스 (AppDatabase)
├── features/
│   ├── auth/
│   ├── notifications/  # 알림 탭 (Phase 1)
│   ├── chat/           # AI 채팅 탭 (Phase 2)
│   ├── analytics/      # 분석 탭 (Phase 3)
│   └── settings/       # 설정 탭 (Phase 3)
├── shared/
│   └── widgets/        # 공통 위젯 (GlassCard, SourceBadge)
└── main.dart
```

### 5. 디자인 시스템 토큰 정의 ✅
- **AppColors**: 다크 모드 + 바이올렛 액센트 컬러 팔레트
- **AppTextStyles**: 타이포그래피 스케일 (Display, Headline, Body, Label)
- **AppSpacing**: 간격 상수 (s4, s8, s12, s16, s20, s24, s32, s40, s48, s64)
- **AppTheme**: Material 3 다크 테마 설정

### 6. 공통 위젯 구현 ✅
- **GlassCard**: 글래스모피즘 효과 카드 위젯
  - `BackdropFilter` + `BoxDecoration` 조합
  - 커스터마이징 가능한 blur, opacity, borderRadius
- **SourceBadge**: 알림 소스 배지 위젯
  - 소스별 아이콘 + 배경색 (Claude, Slack, GitHub, Gmail)
  - 3가지 크기 옵션 (small, medium, large)

### 7. 라우팅 설정 ✅
- `go_router` 기반 라우팅 구성
- 4개 메인 탭: Notifications, Chat, Analytics, Settings
- `BottomNavigationBar` 기반 네비게이션
- Placeholder 화면 생성 완료

### 8. 로컬 DB 설정 ✅
- Drift 초기 설정 완료
- `AppDatabase` 클래스 생성
- 마이그레이션 전략 정의

### 9. 네트워크 클라이언트 설정 ✅
- Dio 클라이언트 설정 완료
- **AuthInterceptor**: JWT 토큰 자동 첨부, 401/5xx 에러 처리
- **LoggingInterceptor**: 디버그 모드 HTTP 로깅

### 10. Phase 0 검증 ✅
- ✅ `flutter analyze`: 경고 1개 (생성 파일의 unused_field, 무시 가능)
- ✅ `flutter test`: 12개 테스트 모두 통과
  - App smoke test
  - GlassCard 위젯 테스트 (5개)
  - SourceBadge 위젯 테스트 (6개)
- ✅ `build_runner` 코드 생성 성공
- ✅ 프로젝트 빌드 준비 완료

## 생성된 파일 목록

### Core
- `lib/core/constants/app_spacing.dart`
- `lib/core/constants/notification_source.dart`
- `lib/core/theme/app_colors.dart`
- `lib/core/theme/app_text_styles.dart`
- `lib/core/theme/app_theme.dart`
- `lib/core/router/app_router.dart`
- `lib/core/router/routes.dart`
- `lib/core/network/dio_client.dart`
- `lib/core/network/auth_interceptor.dart`
- `lib/core/network/logging_interceptor.dart`
- `lib/core/database/app_database.dart`

### Shared
- `lib/shared/widgets/glass_card.dart`
- `lib/shared/widgets/source_badge.dart`

### Features (Placeholder)
- `lib/features/notifications/presentation/notifications_screen.dart`
- `lib/features/chat/presentation/chat_screen.dart`
- `lib/features/analytics/presentation/analytics_screen.dart`
- `lib/features/settings/presentation/settings_screen.dart`

### Main
- `lib/main.dart`

### Tests
- `test/widget_test.dart`
- `test/shared/widgets/glass_card_test.dart`
- `test/shared/widgets/source_badge_test.dart`

## 다음 단계: Phase 1

Phase 1에서는 **인증 및 알림 탭**을 개발합니다:
1. 인증 기능 (로그인, JWT)
2. 알림 데이터 레이어 (Entity, DTO, API)
3. 알림 비즈니스 로직 (Riverpod Notifier)
4. 알림 화면 UI (무한 스크롤, 필터, 스와이프)

자세한 내용은 `/docs/tasks/task_frontend.md`의 Phase 1 섹션을 참조하세요.

---

**Phase 0 완료!** 🎉
