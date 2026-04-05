# Notio Frontend

> AI-powered notification hub for developers - Flutter Frontend

[![Flutter](https://img.shields.io/badge/Flutter-3.27.3-02569B?logo=flutter)](https://flutter.dev)
[![Dart](https://img.shields.io/badge/Dart-3.6.1-0175C2?logo=dart)](https://dart.dev)

**Phase 0 완료** ✅ | Phase 1 개발 중 🚧

## 기술 스택

- **언어**: Dart 3.x
- **프레임워크**: Flutter 3.x
- **상태관리**: hooks_riverpod + flutter_hooks
- **코드 생성**: riverpod_generator + build_runner
- **네비게이션**: go_router
- **네트워크**: dio + retrofit
- **로컬 DB**: drift (SQLite)
- **푸시 알림**: flutter_local_notifications (Firebase는 Phase 4에서 활성화)
- **UI**: flutter_slidable, fl_chart
- **보안**: flutter_secure_storage

## 📁 프로젝트 구조

```
frontend/
├── lib/
│   ├── main.dart                # 앱 엔트리 포인트
│   ├── core/                    # 핵심 기능
│   │   ├── constants/           # 상수 (AppSpacing, NotificationSource)
│   │   ├── database/            # Drift 로컬 DB
│   │   ├── network/             # Dio 클라이언트 및 인터셉터
│   │   ├── router/              # go_router 라우팅
│   │   └── theme/               # 디자인 시스템 (Colors, TextStyles, Theme)
│   ├── features/                # 기능별 모듈 (Feature-first)
│   │   ├── auth/                # 인증 (Phase 1)
│   │   ├── notifications/       # 알림 탭 (Phase 1)
│   │   ├── chat/                # AI 채팅 탭 (Phase 2)
│   │   ├── analytics/           # 분석 탭 (Phase 3)
│   │   └── settings/            # 설정 탭 (Phase 3)
│   └── shared/                  # 공통 컴포넌트
│       └── widgets/             # GlassCard, SourceBadge
├── test/                        # 테스트 파일
│   ├── widget_test.dart         # 앱 스모크 테스트
│   └── shared/widgets/          # 위젯 테스트
├── android/                     # Android 설정
├── ios/                         # iOS 설정
├── macos/                       # macOS 설정
├── web/                         # Web 설정
├── pubspec.yaml                 # 의존성 관리
├── analysis_options.yaml        # 린트 설정
└── PHASE0_COMPLETED.md          # Phase 0 완료 문서
```

### 생성된 파일 (Phase 0)

**Core (11개 파일)**
- `core/constants/app_spacing.dart` - 간격 상수
- `core/constants/notification_source.dart` - 알림 소스 Enum
- `core/theme/app_colors.dart` - 컬러 팔레트
- `core/theme/app_text_styles.dart` - 타이포그래피
- `core/theme/app_theme.dart` - Material 3 테마
- `core/router/app_router.dart` - 라우터 설정
- `core/router/routes.dart` - 라우트 경로
- `core/network/dio_client.dart` - Dio 클라이언트
- `core/network/auth_interceptor.dart` - JWT 인터셉터
- `core/network/logging_interceptor.dart` - 로깅 인터셉터
- `core/database/app_database.dart` - Drift DB

**Shared (2개 파일)**
- `shared/widgets/glass_card.dart` - 글래스모피즘 카드
- `shared/widgets/source_badge.dart` - 소스 배지

**Features (4개 Placeholder 화면)**
- `features/notifications/presentation/notifications_screen.dart`
- `features/chat/presentation/chat_screen.dart`
- `features/analytics/presentation/analytics_screen.dart`
- `features/settings/presentation/settings_screen.dart`

## 🚀 시작하기

### 환경 요구사항

- **Flutter**: 3.27.3
- **Dart**: 3.6.1
- **Xcode**: 16.2+ (macOS/iOS 개발)
- **Android Studio**: 최신 버전 (Android 개발)
- **Chrome**: 최신 버전 (Web 개발)

### 설치 및 실행

```bash
# 1. 의존성 설치
flutter pub get

# 2. 코드 생성 (Riverpod, Drift 등)
dart run build_runner build --delete-conflicting-outputs

# 3. 앱 실행
# macOS
flutter run -d macos

# iOS 시뮬레이터
flutter run -d "iPhone 16 Pro"

# Chrome (빠른 개발/테스트)
flutter run -d chrome

# Android
flutter run -d <device-id>

# 사용 가능한 디바이스 확인
flutter devices
```

### 개발 명령어

```bash
# 코드 분석
flutter analyze

# 테스트 실행
flutter test

# 테스트 커버리지
flutter test --coverage

# 코드 생성 (watch 모드 - 파일 변경 감지)
dart run build_runner watch --delete-conflicting-outputs

# 클린 빌드
flutter clean && flutter pub get
```

### 빌드

```bash
# Android APK (Debug)
flutter build apk --debug

# Android App Bundle (Release)
flutter build appbundle --release

# iOS (Release)
flutter build ios --release

# macOS (Debug)
flutter build macos --debug

# Web
flutter build web
```

### Hot Reload

앱 실행 중 터미널에서:
- `r` - Hot reload (빠른 리로드)
- `R` - Hot restart (전체 재시작)
- `h` - 도움말
- `q` - 종료

## ✅ Phase 0 완료 상태

Phase 0 (환경 세팅 및 기본 인프라)가 **완료**되었습니다!

### 완료된 항목

- ✅ Flutter 프로젝트 초기화
- ✅ 모든 필수 의존성 설치 (Riverpod, go_router, Dio, Drift, Firebase 등)
- ✅ 코드 품질 설정 (flutter_lints, custom_lint, riverpod_lint)
- ✅ Feature-first 폴더 구조 생성
- ✅ 디자인 시스템 토큰 정의
  - AppColors (다크 모드 + 바이올렛 액센트)
  - AppTextStyles (타이포그래피 스케일)
  - AppSpacing (간격 상수)
  - AppTheme (Material 3 다크 테마)
- ✅ 공통 위젯 구현
  - GlassCard (글래스모피즘)
  - SourceBadge (알림 소스 배지)
- ✅ 라우팅 설정 (go_router + BottomNavigationBar)
- ✅ 로컬 DB 설정 (Drift)
- ✅ 네트워크 클라이언트 설정 (Dio + Interceptors)
- ✅ 테스트 작성 (12개 테스트 통과)

### 검증 결과

```bash
✅ flutter analyze: 이슈 없음
✅ flutter test: 12개 테스트 모두 통과
✅ build_runner: 코드 생성 성공
✅ 앱 실행: Chrome/macOS/iOS에서 정상 동작
```

### iOS 호환성 수정 사항

Phase 0 개발 중 Firebase Messaging의 iOS 호환성 문제가 발견되어 다음과 같이 해결했습니다:

- **문제**: `firebase_messaging 14.9.4`가 non-modular header 사용으로 iOS 빌드 실패
- **해결**: Firebase 의존성을 Phase 4까지 일시 비활성화
  - `firebase_core`, `firebase_messaging` 주석 처리
  - iOS 최소 버전 14.0으로 상향
  - Podfile에 `BUILD_LIBRARY_FOR_DISTRIBUTION` 플래그 추가
- **결과**: iPhone 16 Pro 시뮬레이터에서 빌드 성공 (6.7초)

Firebase 푸시 알림 기능은 Phase 4에서 재활성화될 예정입니다.

**상세 내용**: [PHASE0_COMPLETED.md](PHASE0_COMPLETED.md)

## 📊 테스트

```bash
# 모든 테스트 실행
flutter test

# 특정 테스트 실행
flutter test test/shared/widgets/glass_card_test.dart

# 커버리지 포함
flutter test --coverage
```

**현재 상태**: 12개 테스트 모두 통과 ✅

### 테스트 파일
- `test/widget_test.dart` - 앱 스모크 테스트 (1개)
- `test/shared/widgets/glass_card_test.dart` - GlassCard 테스트 (5개)
- `test/shared/widgets/source_badge_test.dart` - SourceBadge 테스트 (6개)

## 🎨 디자인 시스템

### 색상 (AppColors)

```dart
// Primary Colors (Violet Accent)
static const Color primary = Color(0xFF9D4EDD);
static const Color primaryLight = Color(0xFFC77DFF);
static const Color primaryDark = Color(0xFF7B2CBF);

// Background Colors (Dark Mode)
static const Color background = Color(0xFF0A0A0F);
static const Color surface = Color(0xFF1A1A2E);

// Source Badge Colors
static const Color claudeBadge = Color(0xFF9D4EDD);
static const Color slackBadge = Color(0xFFE01E5A);
static const Color githubBadge = Color(0xFF6CC644);
static const Color gmailBadge = Color(0xFFEA4335);
```

### 타이포그래피 (AppTextStyles)

- **Display**: Large (32px), Medium (28px), Small (24px)
- **Headline**: Large (20px), Medium (18px), Small (16px)
- **Body**: Large (16px), Medium (14px), Small (12px)
- **Label**: Large (14px), Medium (12px), Small (10px)

### 간격 (AppSpacing)

```dart
static const double s4 = 4.0;
static const double s8 = 8.0;
static const double s12 = 12.0;
static const double s16 = 16.0;
static const double s20 = 20.0;
static const double s24 = 24.0;
static const double s32 = 32.0;
```

### 공통 위젯

**GlassCard** - 글래스모피즘 효과 카드
```dart
GlassCard(
  blur: 10.0,
  opacity: 0.1,
  borderRadius: 16.0,
  padding: EdgeInsets.all(16),
  child: YourContent(),
)
```

**SourceBadge** - 알림 소스 배지
```dart
SourceBadge(
  source: NotificationSource.claude,
  size: SourceBadgeSize.medium,
)
```

## 📋 개발 규칙

### 네이밍 규칙

| 유형 | 규칙 | 예시 |
|------|------|------|
| 파일명 | snake_case | `notification_card.dart` |
| 화면 Widget | `{Feature}Screen` | `NotificationsScreen` |
| 일반 Widget | 명사 | `GlassCard` |
| Entity | `{Domain}Entity` | `NotificationEntity` |
| Model (DTO) | `{Domain}Model` | `NotificationModel` |
| Provider | camelCase | `notificationsProvider` |
| Notifier | `{Feature}Notifier` | `NotificationsNotifier` |
| Enum | PascalCase | `NotificationSource` |
| Enum 값 | camelCase | `claude`, `slack` |

### 상태관리 (Riverpod)

```dart
// Provider 정의
@riverpod
class NotificationsNotifier extends _$NotificationsNotifier {
  @override
  FutureOr<List<Notification>> build() async {
    return _fetchNotifications();
  }

  Future<void> refresh() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() => _fetchNotifications());
  }
}

// Widget에서 사용
class NotificationsScreen extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notificationsAsync = ref.watch(notificationsNotifierProvider);

    return notificationsAsync.when(
      data: (notifications) => ListView(...),
      loading: () => CircularProgressIndicator(),
      error: (error, stack) => ErrorWidget(error),
    );
  }
}
```

## 🔥 Firebase 설정 (Phase 4)

### Android
1. Firebase Console에서 `google-services.json` 다운로드
2. `android/app/` 디렉토리에 배치
3. `android/app/build.gradle`에 플러그인 추가

### iOS
1. Firebase Console에서 `GoogleService-Info.plist` 다운로드
2. `ios/Runner/` 디렉토리에 배치
3. Xcode에서 프로젝트에 파일 추가

**⚠️ 주의**: Firebase 설정 파일은 `.gitignore`에 포함되어 있습니다.

## 🔧 문제 해결

### 앱이 실행되지 않을 때

```bash
# 1. 올바른 디렉토리 확인
pwd  # /Users/.../notio/frontend 이어야 함

# 2. 클린 빌드
flutter clean && flutter pub get

# 3. 코드 재생성
dart run build_runner build --delete-conflicting-outputs

# 4. 디바이스 확인
flutter devices

# 5. Flutter 환경 확인
flutter doctor -v
```

### 일반적인 문제

**빌드 에러**
- `flutter clean` 후 재시도
- 생성된 파일 삭제 후 `build_runner` 재실행

**의존성 충돌**
- `pubspec.yaml`의 버전 확인
- `flutter pub outdated`로 업데이트 가능 패키지 확인

**Hot Reload 안 됨**
- `R` (대문자) 로 Hot Restart 시도
- 앱 종료 후 재실행

## 📝 다음 단계

### Phase 1: 인증 및 알림 탭 (2주차)

개발 예정 항목:
1. 인증 기능 (로그인, JWT)
2. 알림 데이터 레이어 (Entity, DTO, API)
3. 알림 비즈니스 로직 (Riverpod Notifier)
4. 알림 화면 UI (무한 스크롤, 필터, 스와이프)

자세한 내용은 `/docs/tasks/task_frontend.md`의 **Phase 1** 섹션을 참조하세요.

## 📚 참고 문서

- [프로젝트 개요](../README.md)
- [개발 가이드](../.claude/CLAUDE.md)
- [Frontend 태스크](../docs/tasks/task_frontend.md)
- [Phase 0 완료 문서](PHASE0_COMPLETED.md)

## 📄 라이선스

이 프로젝트는 개인 프로젝트입니다.

---

**Made with ❤️ using Flutter & Claude Code**
