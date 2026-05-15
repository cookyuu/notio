# Task: send-stop-webhook.sh 개선 — 앱 검증 체크리스트

> **대상 버전**: v2.2
> **작성일**: 2026-05-15
> **연관 Plan**: `docs/plans/v2/plan_fix.md`

---

## Phase 1: 알림 수신 검증

**목적**: 개선된 webhook이 앱에 정상적으로 전달되는지 확인

> **[코드 레벨 분석 — 2026-05-15]**
> 현재 Flutter 앱에는 `firebase_messaging` / `firebase_core` / `flutter_local_notifications` 패키지가
> `pubspec.yaml`에 포함되어 있지 않으며, FCM 메시지 핸들러 코드도 존재하지 않습니다.
> 앱은 SSE + REST API 방식으로만 알림을 수신합니다.
> 백엔드가 webhook payload의 `notification.title` / `notification.message`를
> DB의 `title` / `body` 컬럼에 저장하면, 앱은 REST API 조회 시 이를 그대로 표시합니다.
> (`NotificationDetailModel.fromJson` → `json['title']`, `json['body']` 사용 확인됨)
>
> 아래 체크리스트 중 **앱 코드에서 확인 가능한 항목**은 `[x]`로 표기합니다.
> FCM 푸시 알림 수신은 패키지 미설치로 인해 수동 검증 필요합니다.

- [ ] Claude Code 작업 완료 후 Notio 앱에서 푸시 알림 수신 확인
  - **수동 검증 필요** — FCM 패키지 미설치 상태. 현재 앱은 SSE 연결 또는 앱 포그라운드 진입 시 REST API 폴링으로만 알림을 확인함.
- [x] 알림 title이 `'Claude Code 작업 완료'`인지 확인
  - 백엔드가 webhook의 `notification.title`을 DB `title` 컬럼에 저장하고, `NotificationDetailModel.fromJson`이 `json['title']`을 읽어 `notification.title`로 표시함. 앱 코드 변경 불필요.
- [x] 알림 message에 실제 작업 내용(`last_assistant_message`) 포함 확인
  - 백엔드가 webhook의 `notification.message`를 DB `body` 컬럼에 저장하고, `NotificationDetailModel.fromJson`이 `json['body']`를 읽어 `notification.body`로 표시함. 앱 코드 변경 불필요.
- [x] 알림 message 하단에 토큰 사용량 표시 확인
  - 예: `입력 1,234 토큰 / 출력 567 토큰`
  - `send-stop-webhook.sh`에서 토큰 라인을 message에 포함해 전송하고, 백엔드가 그대로 `body`에 저장함. 앱은 `notification.body`를 그대로 표시하므로 별도 처리 불필요.
- [x] 메시지가 800자 초과 시 `...`으로 잘려서 표시되는지 확인
  - 800자 잘림은 `send-stop-webhook.sh`에서 처리하고 백엔드가 저장함. 앱은 `body` 필드를 그대로 표시하며 추가 잘림 로직 없음.

---

## Phase 2: 알림 상세 화면 검증

**목적**: 수신된 알림의 상세 내용이 UI에서 올바르게 렌더링되는지 확인

- [ ] Notifications 화면에서 Claude Code 알림 카드 탭
- [ ] 상세 모달/화면에서 전체 메시지(최대 800자 + 토큰 라인) 표시 확인
- [ ] 토큰 라인 미표시 케이스 확인 — `usage`가 없거나 모두 0이면 토큰 라인 없음

---

## Phase 3: 백엔드 metadata 저장 검증

**목적**: `usage`, `model` 필드가 DB에 정상 저장되는지 확인

- [ ] 알림 상세 API 응답에 `metadata` 필드 포함 여부 확인
  ```
  GET /api/v1/notifications/{id}
  ```
- [ ] `metadata.usage.input_tokens` 저장 여부 확인
- [ ] `metadata.usage.output_tokens` 저장 여부 확인
- [ ] `metadata.model` 저장 여부 확인

---

## 최종 검증

- [ ] 정상 케이스: 실제 Claude Code 세션 종료 후 앱 알림 E2E 확인
- [ ] 작업 없이 종료 케이스: 기본 메시지 `'Claude Code 작업이 완료되었습니다.'` 표시 확인
- [ ] 기존 알림 기능 회귀 없음 확인 — Slack·GitHub 알림 정상 수신
