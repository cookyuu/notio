# Notio Frontend

Flutter 3.x 기반 크로스 플랫폼 모바일 애플리케이션

## 기술 스택

- **언어**: Dart 3.x
- **프레임워크**: Flutter 3.x
- **상태관리**: hooks_riverpod + flutter_hooks
- **코드 생성**: riverpod_generator + build_runner
- **네비게이션**: go_router
- **네트워크**: dio + retrofit
- **로컬 DB**: drift (SQLite)
- **푸시 알림**: firebase_messaging + flutter_local_notifications
- **UI**: flutter_slidable, fl_chart
- **보안**: flutter_secure_storage

## 프로젝트 구조

```
frontend/
├── lib/
│   ├── main.dart
│   ├── core/                    # 코어 유틸리티
│   │   ├── config/             # 설정
│   │   ├── network/            # API 클라이언트
│   │   ├── database/           # Drift DB
│   │   └── utils/              # 유틸리티
│   ├── features/               # 기능별 모듈
│   │   ├── auth/               # 인증
│   │   ├── notifications/      # 알림 목록
│   │   ├── chat/               # AI 채팅
│   │   ├── todos/              # 할일 관리
│   │   ├── analytics/          # 분석
│   │   └── settings/           # 설정
│   └── shared/                 # 공유 컴포넌트
│       ├── widgets/            # 재사용 위젯
│       ├── models/             # 데이터 모델
│       └── theme/              # 디자인 시스템
├── pubspec.yaml
└── analysis_options.yaml
```

## 시작하기

### 사전 요구사항

- Flutter 3.x
- Dart 3.x
- Android Studio / Xcode (플랫폼별)

### 설치 및 실행

1. **의존성 설치**
```bash
flutter pub get
```

2. **코드 생성**
```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

3. **애플리케이션 실행**
```bash
# 개발 모드
flutter run

# 특정 디바이스
flutter run -d <device_id>

# 디바이스 목록 확인
flutter devices
```

### 빌드

```bash
# Android APK
flutter build apk --debug

# Android App Bundle (프로덕션)
flutter build appbundle --release

# iOS
flutter build ios --release
```

## 개발

### 코드 생성

Riverpod, Retrofit, Drift 등을 사용하는 경우 코드 생성이 필요합니다:

```bash
# 일회성 생성
flutter pub run build_runner build --delete-conflicting-outputs

# 자동 생성 (파일 변경 감지)
flutter pub run build_runner watch --delete-conflicting-outputs
```

### 테스트

```bash
# 전체 테스트 실행
flutter test

# 특정 테스트 실행
flutter test test/features/notifications_test.dart

# 커버리지
flutter test --coverage
```

### 코드 품질

```bash
# 분석 (경고 0개 목표)
flutter analyze

# 포맷팅
flutter format lib/

# 린트 규칙 확인
flutter pub run custom_lint
```

## 개발 규칙

### 파일 및 네이밍
| 유형 | 규칙 | 예시 |
|------|------|------|
| 파일명 | snake_case | `notification_card.dart` |
| 화면 Widget | `{Feature}Screen` | `NotificationsScreen` |
| 일반 Widget | `{기능}Widget` 또는 `{기능}` | `GlassCard` |
| Entity | `{Domain}Entity` | `NotificationEntity` |
| Model (DTO) | `{Domain}Model` | `NotificationModel` |
| Provider | `{feature}Provider` (camelCase) | `notificationsProvider` |
| Notifier | `{Feature}Notifier` | `NotificationsNotifier` |

### 디렉토리 구조
```
features/
└── notifications/
    ├── data/
    │   ├── models/
    │   ├── repositories/
    │   └── datasources/
    ├── domain/
    │   ├── entities/
    │   └── repositories/
    └── presentation/
        ├── screens/
        ├── widgets/
        └── providers/
```

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

### 디자인 시스템

모든 색상, 타이포그래피, 간격은 디자인 시스템에서만 정의합니다:

```dart
// shared/theme/app_colors.dart
class AppColors {
  static const primary = Color(0xFF8B5CF6);  // Violet
  static const background = Color(0xFF0A0A0A);
  // ...
}

// shared/theme/app_text_styles.dart
class AppTextStyles {
  static const headline1 = TextStyle(...);
  static const body1 = TextStyle(...);
}

// shared/theme/app_spacing.dart
class AppSpacing {
  static const s4 = 4.0;
  static const s8 = 8.0;
  static const s16 = 16.0;
}
```

### 글래스모피즘

직접 `Container`를 작성하지 말고 `GlassCard` 위젯을 재사용합니다:

```dart
GlassCard(
  child: YourContent(),
)
```

## Firebase 설정

### Android
1. `google-services.json`을 `android/app/` 디렉토리에 배치
2. `android/app/build.gradle`에 플러그인 추가

### iOS
1. `GoogleService-Info.plist`를 `ios/Runner/` 디렉토리에 배치
2. Xcode에서 프로젝트에 파일 추가

## 환경 변수

```dart
// lib/core/config/env.dart
class Env {
  static const baseUrl = String.fromEnvironment(
    'BASE_URL',
    defaultValue: 'http://localhost:8080',
  );
}
```

실행 시:
```bash
flutter run --dart-define=BASE_URL=https://api.notio.app
```

## 문의

상세한 개발 가이드는 [`../.claude/claude.md`](../.claude/claude.md)를 참조하세요.
