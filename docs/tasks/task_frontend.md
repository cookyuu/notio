# Frontend 개발 체크리스트

> Flutter 3.x · Dart 3.6 · Riverpod · 단계별 개발 가이드

---

## Phase 0: 환경 세팅 및 기본 인프라 (1주차)

### 프로젝트 초기화
- [x] Flutter 3.x 프로젝트 생성
- [x] Dart 3.6.x 설정
- [x] `.gitignore` 설정 (Flutter 표준 + 커스텀)
- [x] `.editorconfig` 설정

### 의존성 설정
- [x] `pubspec.yaml` 의존성 추가
  - [x] `hooks_riverpod: 2.5.3`
  - [x] `flutter_hooks: 0.20.5`
  - [x] `riverpod_generator: 2.6.2`
  - [x] `build_runner: 2.4.13`
  - [x] `go_router: 14.6.1`
  - [x] `dio: 5.4.3`
  - [x] `retrofit: 4.1.0`
  - [x] `json_annotation: 4.9.0`
  - [x] `json_serializable: 6.8.0`
  - [x] `drift: 2.18.0`
  - [x] `drift_dev: 2.18.0`
  - [x] `firebase_core: 2.27.2` (Phase 4를 위해 임시 비활성화)
  - [x] `firebase_messaging: 14.7.20` (Phase 4를 위해 임시 비활성화)
  - [x] `flutter_local_notifications: 17.2.3`
  - [x] `flutter_slidable: 3.1.1`
  - [x] `fl_chart: 0.68.0`
  - [x] `flutter_secure_storage: 9.2.2`
  - [x] `timeago: 3.6.1`
  - [x] `flutter_lints: 5.0.0`
  - [x] `custom_lint: 0.7.3`
  - [x] `riverpod_lint: 2.6.0`
- [x] `flutter pub get` 실행 확인
- [x] `build_runner` 설정 및 실행 확인

### 코드 품질 설정
- [x] `analysis_options.yaml` 설정
  - [x] `flutter_lints` 활성화
  - [x] `custom_lint` 규칙 추가
  - [x] `riverpod_lint` 규칙 추가
  - [x] 커스텀 린트 규칙 정의
- [ ] `flutter analyze` 실행 확인 (경고 0개)

### 프로젝트 구조 생성
- [x] Feature-first 폴더 구조 생성
  ```
  lib/
  ├── core/
  │   ├── constants/       ✅
  │   ├── theme/           ✅
  │   ├── utils/           (필요시 추가)
  │   ├── router/          ✅
  │   ├── network/         ✅
  │   └── database/        ✅
  ├── features/
  │   ├── auth/            (Phase 1에서 구현 필요)
  │   ├── notifications/   ✅ (presentation만 구현)
  │   ├── chat/            ✅ (presentation만 구현)
  │   ├── analytics/       ✅ (presentation만 구현)
  │   └── settings/        ✅ (presentation만 구현)
  ├── shared/
  │   └── widgets/         ✅
  └── main.dart            ✅
  ```

### 디자인 시스템 토큰 정의
- [x] `AppColors` 정의 (`lib/core/theme/app_colors.dart`)
  - [x] 다크 모드 컬러 팔레트 (바이올렛 액센트)
  - [x] 시맨틱 컬러 (primary, secondary, error, success 등)
- [x] `AppTextStyles` 정의 (`lib/core/theme/app_text_styles.dart`)
  - [x] 타이포그래피 스케일 (headline, body, caption 등)
- [x] `AppSpacing` 정의 (`lib/core/constants/app_spacing.dart`)
  - [x] s4, s8, s12, s16, s20, s24, s32 상수

### 공통 위젯 구현
- [x] `GlassCard` 위젯 구현 (글래스모피즘)
  - [x] `backdrop_filter` + `BoxDecoration` 조합
  - [x] 파라미터: `blur`, `opacity`, `border`, `child`
  - [x] 골든 테스트 작성 (3개 시나리오)
- [x] `SourceBadge` 위젯 구현
  - [x] 소스별 아이콘 + 배경색
  - [x] Enum: `NotificationSource` (claude, slack, github, gmail)
  - [x] 위젯 테스트 작성 (6개 테스트)

### 라우팅 설정
- [x] `AppRouter` 구성 (`go_router`)
  - [x] 라우트 정의: `/notifications`, `/chat`, `/analytics`, `/settings`
  - [x] 네비게이션 바 구조 (NavigationBar)
  - [ ] 딥 링크 설정 (푸시 알림 대응) - Phase 4에서 구현

### 로컬 DB 설정
- [x] Drift 초기 설정
  - [x] `AppDatabase` 클래스 생성
  - [x] 마이그레이션 전략 정의
  - [ ] DAO 기본 구조 설정 (Phase 1에서 테이블 정의와 함께 구현)

### 네트워크 클라이언트 설정
- [x] Dio 클라이언트 설정
  - [x] Base URL 설정 (환경변수)
  - [x] `AuthInterceptor` 구현
    - [x] JWT 토큰 자동 첨부
    - [x] 401 에러 시 로그아웃 (TODO 주석으로 표시)
    - [x] 5xx 에러 시 재시도 (TODO 주석으로 표시, Phase 1에서 완성)
  - [x] `LoggingInterceptor` 추가 (디버그 모드)

### Phase 0 검증
- [x] `flutter analyze` 경고 0개 ✅
- [x] `flutter test` 모든 테스트 통과 (15개 테스트 성공) ✅
- [x] `flutter build apk --debug` 성공 (커밋 로그에서 확인) ✅
- [ ] 앱 실행 및 기본 네비게이션 동작 확인 (실기기에서 테스트 필요)

**Phase 0 상태**: ✅ **완료** (실기기 테스트 제외 모든 항목 완료)

---

## Phase 1: 인증 및 알림 탭 (2주차)

**Phase 1 상태**: 인증 기능 구현 완료 (목업 데이터), 알림 탭은 미구현

### 인증 기능
- [x] `AuthEntity` + Drift 테이블 정의
- [x] `LoginRequest` / `LoginResponse` DTO 정의
- [x] `AuthApiClient` (Retrofit) 구현
  - [x] `POST /api/v1/auth/login`
  - [x] `POST /api/v1/auth/refresh`
  - [x] `POST /api/v1/auth/logout`
- [x] `AuthRepository` + `AuthRepositoryImpl` 구현
  - [x] 토큰 저장 (`flutter_secure_storage`)
  - [x] 자동 로그인 체크
  - [x] 목업 데이터 사용 (백엔드 API 준비 전까지)
- [x] `AuthNotifier` (Riverpod AsyncNotifier) 구현
- [x] `LoginScreen` UI 구현 (간단한 폼)
  - [x] 이메일/비밀번호 입력
  - [x] 로딩 상태 표시
  - [x] 에러 메시지 표시
  - [x] 목업 로그인 안내 표시
- [x] 라우터 인증 가드 구현 (go_router redirect)

### 알림 데이터 레이어
- [x] `NotificationEntity` + Drift 테이블 정의 ✅
  - [x] 필드: id, title, body, source, priority, isRead, createdAt, externalId, externalUrl, metadata
  - [x] NotificationPriority enum 정의 (LOW, MEDIUM, HIGH, URGENT)
  - [x] 인메모리 캐시로 구현 (Drift는 build_runner 호환성 문제로 Phase 4에서 재구현)
- [x] `NotificationModel` DTO 정의 (JSON 직렬화) ✅
  - [x] 수동 fromJson/toJson 구현
  - [x] Entity 변환 메서드 (toEntity, fromEntity)
- [x] `NotificationRemoteDataSource` 구현 ✅
  - [x] `GET /api/v1/notifications` (페이지네이션) - 목업 구현
  - [x] `GET /api/v1/notifications/unread-count` - 목업 구현
  - [x] `PATCH /api/v1/notifications/{id}/read` - 목업 구현
  - [x] `PATCH /api/v1/notifications/read-all` - 목업 구현
  - [x] 백엔드 API 준비 전까지 1개의 Slack 알림 목업 데이터 사용
- [x] `NotificationLocalDataSource` 구현 ✅
  - [x] 인메모리 Map 기반 캐싱
  - [x] getCachedNotifications (소스 필터링 지원)
  - [x] cacheNotifications
  - [x] markAsRead / markAllAsRead
  - [x] getNotificationById
  - [x] deleteNotification
- [x] `NotificationRepository` + `NotificationRepositoryImpl` 구현 ✅
  - [x] 로컬 캐시 + 원격 API 동기화
  - [x] `fetchNotifications(filter, page)` - 원격 실패 시 캐시 fallback
  - [x] `markAsRead(id)` - 낙관적 업데이트
  - [x] `markAllAsRead()` - 낙관적 업데이트
  - [x] `deleteNotification(id)`
  - [x] 오프라인 시 로컬 우선 조회
- [x] 단위 테스트 작성 및 통과 ✅
  - [x] NotificationRepository 테스트 (6개)
  - [x] Mock Data 테스트 (3개)
  - [x] 총 9개 테스트 모두 통과
- [x] `MOCK_DATA_INFO.md` 문서 작성 ✅
  - [x] 더미 데이터 상세 정보
  - [x] 사용 예시
  - [x] UI 표시 가이드

### 알림 비즈니스 로직
- [x] `NotificationsNotifier` (Riverpod StateNotifier) 구현 ✅
  - [x] 페이지네이션 상태 관리 (무한 스크롤)
  - [x] 필터 상태 (source)
  - [x] 미읽음 수 실시간 갱신 (unreadCountProvider)
  - [x] 낙관적 업데이트 (읽음 처리, 삭제)
  - [x] NotificationsState 정의 (notifications, isLoading, error, selectedSource, page, hasMore)

### 알림 화면 UI
- [x] `NotificationsScreen` 구현 ✅
  - [x] 상단 필터 칩 (전체, Claude, Slack, GitHub, Gmail)
  - [x] 무한 스크롤 리스트 (`ListView.builder` + `ScrollController`)
  - [x] Pull-to-refresh (RefreshIndicator)
  - [x] 미읽음 수 배지 (AppBar, unreadCountProvider)
  - [x] 전체 읽음 버튼 (AppBar, IconButton)
- [x] `NotificationCard` 위젯 구현 ✅
  - [x] `GlassCard` 기반 디자인 (InkWell로 감싸서 터치 효과)
  - [x] 소스 배지 (`SourceBadge`)
  - [x] 우선순위 인디케이터 (왼쪽 색상 바)
  - [x] 읽음/미읽음 시각적 구분 (텍스트 색상, 볼드, 보라색 점)
  - [x] 타임스탬프 (`timeago` 한글 로케일)
- [x] 스와이프 동작 (`flutter_slidable`) ✅
  - [x] 오른쪽 스와이프 → 읽음 처리 / 삭제
  - [ ] 왼쪽 스와이프 → 할일 생성 (Phase 2에서 연동)
- [ ] 상세 모달 (알림 클릭 시) - TODO 주석으로 표시
  - [ ] 전체 내용 표시
  - [ ] 읽음 처리 자동 (탭 시 markAsRead 구현됨)
  - [ ] 관련 링크 렌더링
- [x] Empty State UI (알림 없을 때) ✅
- [x] 로딩/에러 상태 UI ✅

### Phase 1 검증
- [ ] 로그인 → 알림 목록 조회 플로우 테스트
- [ ] 알림 클릭 → 상세 모달 → 읽음 처리 확인
- [ ] 스와이프 동작 확인 (읽음, 삭제)
- [ ] 필터링 동작 확인 (소스별 필터)
- [ ] 무한 스크롤 동작 확인
- [ ] 오프라인 → 로컬 캐시 조회 확인
- [ ] `flutter test` 모든 테스트 통과

---

## Phase 2: AI 채팅 탭 (3주차)

**Phase 2 상태**: ✅ **완료** (더미 데이터 기반 구현)

### 채팅 데이터 레이어
- [x] `ChatMessageEntity` + Drift 테이블 정의 ✅
  - [x] 필드: id, role (user/assistant), content, createdAt
  - [x] 최근 50개만 유지 (인메모리 캐시로 구현)
- [x] `ChatRequest` / `ChatResponse` DTO 정의 ✅
- [x] `DailySummaryModel` DTO 정의 ✅
- [x] `ChatApiClient` (Remote DataSource) 구현 ✅
  - [x] `POST /api/v1/chat` — 단건 응답 (더미 데이터)
  - [x] `GET /api/v1/chat/stream` — SSE 스트리밍 (더미 스트리밍)
  - [x] `GET /api/v1/chat/daily-summary` — 오늘 요약 (더미 데이터)
  - [x] `GET /api/v1/chat/history` — 채팅 이력 (더미 데이터)

### 채팅 비즈니스 로직
- [x] `ChatRepository` + `ChatRepositoryImpl` 구현 ✅
  - [x] 로컬 인메모리 캐시 (최근 50개)
  - [x] `sendMessage(content)` — 단건 전송
  - [x] `streamMessage(content)` — SSE 연결 및 점진적 수신
  - [x] `fetchDailySummary()` — 오늘 요약 조회
  - [x] `fetchHistory(page)` — 채팅 이력 조회
- [x] `ChatNotifier` (Riverpod StateNotifier) 구현 ✅
  - [x] 채팅 메시지 목록 상태
  - [x] 스트리밍 상태 (typing indicator)
  - [x] 오늘 요약 Provider 분리 (dailySummaryProvider)
  - [x] 메시지 전송 중 상태 관리

### 채팅 화면 UI
- [x] `ChatScreen` 구현 ✅
  - [x] 상단 오늘 요약 카드 (DailySummaryCard)
    - [x] 접기/펼치기 애니메이션
    - [x] 24시간 캐시 표시
  - [x] 채팅 메시지 리스트
    - [x] 사용자 메시지 (오른쪽 정렬, 바이올렛 배경)
    - [x] AI 메시지 (왼쪽 정렬, 글래스 배경)
    - [x] 타임스탬프 (`timeago`)
    - [x] 아바타 아이콘 (사용자/AI)
  - [x] 하단 입력창 (ChatInputField)
    - [x] `TextField` + 전송 버튼
    - [x] 멀티라인 지원
    - [x] 전송 중 버튼 비활성화
  - [x] SSE 스트리밍 시 점진적 렌더링 (StreamingMessageBubble)
    - [x] 스트리밍 중 인디케이터 (점 애니메이션)
- [x] Empty State UI (채팅 없을 때) ✅
  - [x] 환영 메시지
  - [x] 예시 질문 칩
- [x] 로딩/에러 상태 UI ✅

### SSE 스트리밍 구현
- [x] SSE 연결 관리 ✅
  - [x] Stream 기반 더미 스트리밍 구현
  - [ ] `EventSource` 래퍼 클래스 (백엔드 API 준비 후 구현)
  - [ ] 타임아웃 30초 (백엔드 API 준비 후 구현)
  - [ ] Exponential Backoff 재연결 (백엔드 API 준비 후 구현)
- [x] 점진적 메시지 렌더링 ✅
  - [x] Stream 기반 상태 업데이트
  - [x] 단어 단위 애니메이션 (50ms 간격)

### 테스트
- [x] 단위 테스트 작성 및 통과 ✅
  - [x] ChatRepository 테스트 (7개)
  - [x] Mock Data 테스트 (4개)
  - [x] 총 35개 테스트 모두 통과

### Phase 2 검증
- [x] `flutter test` 모든 테스트 통과 (35/35) ✅
- [x] AI 채팅 메시지 전송 → 단건 응답 수신 구현 ✅
- [x] SSE 스트리밍 응답 구현 (더미 데이터) ✅
- [x] 오늘 요약 조회 구현 ✅
- [x] 채팅 이력 조회 구현 ✅
- [x] 로컬 캐시 조회 구현 ✅
- [ ] 실기기 테스트 (수동 테스트 필요)
- [ ] 타임아웃/재연결 동작 (백엔드 API 준비 후 구현)

**참고사항**:
- 백엔드 API가 준비될 때까지 더미 데이터 사용
- Drift 데이터베이스는 Phase 4에서 재구현 예정 (현재 인메모리 캐시)
- SSE 실제 연결은 백엔드 준비 후 구현 예정

---

## Phase 3: 분석 및 설정 탭 (4주차)

### 분석 데이터 레이어
- [ ] `WeeklyAnalyticsModel` DTO 정의
  - [ ] 필드: 주간 알림 수, 소스별 분포, 우선순위별 분포, 일별 트렌드, 인사이트
- [ ] `AnalyticsApiClient` (Retrofit) 구현
  - [ ] `GET /api/v1/analytics/weekly`
- [ ] `AnalyticsRepository` + `AnalyticsRepositoryImpl` 구현
  - [ ] `fetchWeeklySummary()`
  - [ ] 캐싱 전략 (1시간 TTL)

### 분석 비즈니스 로직
- [ ] `AnalyticsNotifier` (Riverpod AsyncNotifier) 구현
  - [ ] 주간 통계 상태
  - [ ] 차트 데이터 변환 로직

### 분석 화면 UI
- [ ] `AnalyticsScreen` 구현
  - [ ] 주간 알림 통계 카드
    - [ ] 총 알림 수, 미읽음 수, 우선순위별 요약
  - [ ] 소스별 분포 도넛 차트 (`fl_chart`)
    - [ ] 인터랙티브 터치 효과
    - [ ] 범례 표시
  - [ ] 우선순위별 막대 차트 (`fl_chart`)
  - [ ] 일별 트렌드 라인 차트 (`fl_chart`)
    - [ ] 7일 데이터
    - [ ] 그리드 라인
  - [ ] 인사이트 텍스트 (LLM 생성)
    - [ ] `GlassCard`로 감싸기
    - [ ] 아이콘 + 텍스트
- [ ] Empty State UI (데이터 없을 때)
- [ ] 로딩/에러 상태 UI

### 설정 데이터 레이어
- [ ] `SettingsModel` 정의
  - [ ] 필드: isDarkMode, isPushEnabled, defaultFilter
- [ ] `SettingsRepository` + `SettingsRepositoryImpl` 구현
  - [ ] `SharedPreferences` 기반 저장
  - [ ] `loadSettings()`
  - [ ] `saveSettings(settings)`

### 설정 비즈니스 로직
- [ ] `SettingsNotifier` (Riverpod Notifier) 구현
  - [ ] `toggleDarkMode()`
  - [ ] `togglePushNotification()`
  - [ ] `setDefaultFilter(filter)`

### 설정 화면 UI
- [ ] `SettingsScreen` 구현
  - [ ] 다크 모드 토글 (기본값: true)
    - [ ] Switch 위젯
    - [ ] 앱 전체 테마 즉시 적용
  - [ ] 푸시 알림 설정 (On/Off)
  - [ ] 알림 필터 기본값 설정
    - [ ] 드롭다운 메뉴
  - [ ] 로그아웃 버튼
    - [ ] 확인 다이얼로그
    - [ ] 토큰 삭제 + 로그인 화면 이동
  - [ ] 버전 정보 표시
    - [ ] `package_info_plus`로 버전 조회
  - [ ] 섹션 구분 (ListTile 그룹)

### 다크 모드 전환
- [ ] `ThemeNotifier` 구현
- [ ] `MaterialApp`에서 테마 동적 적용
- [ ] 설정 변경 시 앱 전체 테마 갱신 확인

### Phase 3 검증
- [ ] 분석 탭 주간 통계 조회 확인
- [ ] 차트 인터랙션 동작 확인 (터치, 줌 등)
- [ ] 다크 모드 토글 → 앱 전체 테마 즉시 적용 확인
- [ ] 푸시 알림 설정 변경 확인
- [ ] 로그아웃 → 토큰 삭제 → 로그인 화면 이동 확인
- [ ] `flutter test` 모든 테스트 통과

---

## Phase 4: 푸시 알림 및 통합 (5주차)

### Firebase 설정
- [ ] Firebase 프로젝트 생성
- [ ] Android 설정
  - [ ] `google-services.json` 추가
  - [ ] `build.gradle` 설정
  - [ ] `AndroidManifest.xml` 권한 추가
- [ ] iOS 설정
  - [ ] `GoogleService-Info.plist` 추가
  - [ ] APNs 인증 키 업로드
  - [ ] `Info.plist` 권한 추가
  - [ ] Runner 타겟 설정

### FCM 토큰 관리
- [ ] `DeviceApiClient` (Retrofit) 구현
  - [ ] `POST /api/v1/devices/register`
  - [ ] `DELETE /api/v1/devices/unregister`
- [ ] `FirebaseMessaging.instance.getToken()` — FCM 토큰 발급
- [ ] 토큰 발급 시 백엔드 등록
- [ ] 토큰 갱신 시 자동 재등록

### 푸시 알림 처리
- [ ] 포그라운드 알림 처리
  - [ ] `flutter_local_notifications` 설정
  - [ ] 알림 채널 생성 (Android)
  - [ ] 알림 수신 시 로컬 알림 표시
- [ ] 백그라운드 알림 처리
  - [ ] `@pragma('vm:entry-point')` 핸들러 구현
  - [ ] 백그라운드에서 데이터 동기화
- [ ] 알림 클릭 처리
  - [ ] 페이로드 파싱
  - [ ] 해당 화면 라우팅 (딥 링크)
  - [ ] 알림 상세 모달 자동 오픈
- [ ] 알림 권한 요청 UI
  - [ ] 최초 실행 시 권한 요청 다이얼로그
  - [ ] 거부 시 설정 유도

### 로컬 캐시 동기화
- [ ] `NotificationEntity` 테이블 최적화
  - [ ] 최근 100개만 유지
  - [ ] 24시간 TTL 기반 자동 정리
- [ ] `ChatMessageEntity` 테이블 최적화
  - [ ] 최근 50개만 유지
- [ ] 오프라인 모드
  - [ ] 로컬 데이터 우선 조회
  - [ ] 네트워크 에러 시 캐시 표시
- [ ] 온라인 복구
  - [ ] 자동 동기화 트리거
  - [ ] 변경 사항 병합

### 통합 테스트
- [ ] 로그인 플로우
  - [ ] 로그인 → FCM 토큰 등록 → 알림 목록 조회
- [ ] 알림 플로우
  - [ ] 알림 수신 → 읽음 처리 → 삭제
  - [ ] 알림 클릭 → 상세 모달 → 읽음 처리
- [ ] 채팅 플로우
  - [ ] 메시지 전송 → SSE 스트리밍 응답 수신
  - [ ] 오늘 요약 조회 → 캐싱 동작 확인
- [ ] 푸시 알림 플로우
  - [ ] 푸시 수신 → 클릭 → 해당 화면 이동
  - [ ] 포그라운드/백그라운드 알림 수신
- [ ] 오프라인/온라인 플로우
  - [ ] 오프라인 → 로컬 캐시 조회
  - [ ] 온라인 복구 → 자동 동기화
- [ ] 설정 플로우
  - [ ] 다크 모드 토글 → 전체 테마 적용
  - [ ] 푸시 알림 Off → FCM 토큰 삭제
  - [ ] 로그아웃 → 토큰 삭제 → 로그인 화면

### 실기기 테스트
- [ ] Android 실기기 테스트
  - [ ] 푸시 알림 수신 확인 (포그라운드/백그라운드)
  - [ ] 알림 클릭 → 라우팅 확인
  - [ ] 오프라인 모드 확인
- [ ] iOS 실기기 테스트
  - [ ] 푸시 알림 수신 확인 (포그라운드/백그라운드)
  - [ ] 알림 클릭 → 라우팅 확인
  - [ ] 오프라인 모드 확인

### Phase 4 검증
- [ ] `flutter analyze` 경고 0개
- [ ] `flutter test` 모든 테스트 통과
- [ ] `flutter build apk --release` 성공
- [ ] `flutter build ios --release` 성공 (macOS)
- [ ] 모든 통합 테스트 시나리오 통과
- [ ] 실기기 테스트 (Android + iOS) 통과

---

## Phase 5: 코드 품질 및 CI/CD (최종)

### 테스트 커버리지
- [ ] 단위 테스트 작성
  - [ ] `NotificationCard` 위젯 테스트
  - [ ] `GlassCard` 골든 테스트
  - [ ] `SourceBadge` 위젯 테스트
  - [ ] Notifier 로직 테스트
  - [ ] Repository 테스트 (Mock)
- [ ] 통합 테스트 작성
  - [ ] 주요 플로우 E2E 테스트
- [ ] 테스트 커버리지 측정
  - [ ] `flutter test --coverage`
  - [ ] 목표: 80% 이상

### 코드 품질 점검
- [ ] `flutter analyze` 모든 경고 해결
- [ ] `flutter_lints` 규칙 100% 준수
- [ ] `riverpod_lint` 규칙 준수
- [ ] 코드 리뷰 체크리스트 작성
  - [ ] 네이밍 규칙 준수
  - [ ] 디자인 시스템 사용 (하드코딩 금지)
  - [ ] 에러 처리 완전성

### 성능 최적화
- [ ] 앱 시작 시간 측정
- [ ] 프레임 드롭 측정 (DevTools)
- [ ] 이미지 최적화 (압축)
- [ ] 불필요한 rebuild 제거 (`ConsumerWidget` vs `Consumer`)
- [ ] 리스트 성능 최적화 (`ListView.builder`)

### 빌드 최적화
- [ ] ProGuard 설정 (Android)
  - [ ] `proguard-rules.pro` 작성
  - [ ] 난독화 테스트
- [ ] App Bundle 크기 측정
  - [ ] 목표: 20MB 이하 (Android)
- [ ] `flutter build apk --release` 최종 확인
- [ ] `flutter build ios --release` 최종 확인

### CI/CD 설정
- [ ] GitHub Actions 워크플로우 작성
  - [ ] `.github/workflows/ci-frontend.yml`
  - [ ] 트리거: `frontend/**` 변경 시만 실행
  - [ ] 단계:
    - [ ] Flutter 설치
    - [ ] 의존성 설치 (`flutter pub get`)
    - [ ] 코드 분석 (`flutter analyze`)
    - [ ] 테스트 실행 (`flutter test`)
    - [ ] 빌드 (`flutter build apk --debug`)
- [ ] CI 실행 확인 (PR 생성 시)

### 문서화
- [ ] README 작성
  - [ ] 프로젝트 개요
  - [ ] 환경 세팅 가이드
  - [ ] 빌드 및 실행 방법
  - [ ] 폴더 구조 설명
- [ ] API 문서 연동
  - [ ] Swagger 엔드포인트 명시
- [ ] 디자인 시스템 문서 작성
  - [ ] `AppColors`, `AppTextStyles`, `AppSpacing` 사용법

### 최종 점검
- [ ] 모든 Phase 체크리스트 완료 확인
- [ ] 실기기 최종 테스트 (Android + iOS)
- [ ] 백엔드 통합 테스트 완료
- [ ] 푸시 알림 End-to-End 테스트 완료
- [ ] 오프라인 모드 완전 동작 확인
- [ ] 성능 지표 목표 달성 (프레임 드롭 < 5%)
- [ ] 보안 체크리스트 완료
  - [ ] 토큰 안전하게 저장 (`flutter_secure_storage`)
  - [ ] API 키 노출 방지 (환경변수)
  - [ ] `.gitignore` 확인

---

## 완료 기준 (Definition of Done)

각 Phase 완료 시:
- [ ] 해당 Phase의 모든 체크리스트 항목 완료
- [ ] `flutter analyze` 경고 0개
- [ ] `flutter test` 모든 테스트 통과
- [ ] 실기기 동작 확인 (주요 플로우)
- [ ] 코드 리뷰 완료 (팀원 또는 셀프 리뷰)
- [ ] Git 커밋 및 PR 생성
- [ ] CI 통과

MVP 완료 시 (Phase 5 종료):
- [ ] 모든 Phase 완료 기준 충족
- [ ] 백엔드와 통합 테스트 완료
- [ ] 푸시 알림 End-to-End 동작 확인
- [ ] 프로덕션 빌드 성공 (Android + iOS)
- [ ] 앱 스토어 제출 준비 완료 (스크린샷, 설명 등)

---

**이제 Phase 0부터 차근차근 시작하세요!** 🚀
