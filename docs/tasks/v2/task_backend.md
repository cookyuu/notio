# Task: send-stop-webhook.sh 개선 체크리스트

> **대상 버전**: v2.2
> **작성일**: 2026-05-15
> **연관 Plan**: `docs/plans/v2/plan_fix.md`
> **수정 대상**: `.claude/hooks/send-stop-webhook.sh`

---

## Phase 1: Python stdin 파이프 방식 전환

**목적**: 따옴표·특수문자 포함 입력에서 파싱 실패하는 쉘 변수 삽입 취약점 제거

- [x] `json.loads('''$INPUT''')` → `json.load(sys.stdin)` 방식으로 전환
  - [x] `echo "$INPUT" | python3 -c "import json, sys; data = json.load(sys.stdin) ..."` 구조로 변경
  - [x] Python 스크립트 내에서 `$INPUT` 직접 참조 제거

---

## Phase 2: last_assistant_message 추출 + transcript fallback

**목적**: 고정 메시지 대신 실제 작업 내용을 알림에 포함

- [x] StopHook 직접 제공 필드 우선 추출
  - [x] `last_assistant_message = data.get('last_assistant_message', '')`
- [x] `last_assistant_message`가 비어있을 경우 transcript fallback 구현
  - [x] `transcript_path = data.get('transcript_path', '')` 추출
  - [x] `os.path.exists(transcript_path)` 확인 후 JSONL 파일 파싱
  - [x] `role == 'assistant'`인 마지막 entry의 text block(`type == 'text'`) 추출
  - [x] 파싱 실패 시 개별 라인 `try/except`로 건너뜀
- [x] 모두 실패 시 기본 메시지 `'Claude Code 작업이 완료되었습니다.'` 사용

---

## Phase 3: 메시지 포맷 구성

**목적**: 작업 요약 + 토큰 사용량을 하나의 메시지 문자열로 조합

- [x] `summary` = `last_assistant_message` 최대 800자 트리밍
  - [x] 800자 초과 시 `'...'` 접미 추가
- [x] `token_line` 생성
  - [x] `input_tokens == 0` and `output_tokens == 0` 이면 `token_line = ''`
  - [x] 아닐 경우 `f'\n\n입력 {input_tokens:,} 토큰 / 출력 {output_tokens:,} 토큰'`
- [x] `message = (summary or 기본메시지) + token_line`

---

## Phase 4: payload에 usage, model 추가

**목적**: 백엔드가 metadata JSONB에 저장할 수 있도록 payload 확장

- [x] `usage.input_tokens`, `usage.output_tokens` 추출
  - [x] `usage = data.get('usage', {})`
  - [x] `input_tokens = usage.get('input_tokens', 0)`
  - [x] `output_tokens = usage.get('output_tokens', 0)`
- [x] `model = data.get('model', '')` 추출
- [x] payload에 `'usage'` 객체 추가
  ```json
  "usage": { "input_tokens": 1234, "output_tokens": 567 }
  ```
- [x] payload에 `'model'` 문자열 추가

---

## Phase 5: 백엔드 webhook 핸들러 확인

**목적**: 새 필드(`usage`, `model`)가 metadata JSONB에 정상 저장되는지 확인

- [x] Claude webhook handler에서 `usage`, `model` 필드를 `metadata` 컬럼에 저장하는 로직 확인
  - [x] 이미 처리 중이면 변경 없음 — 확인만
    > `ClaudeWebhookHandler`가 payload 전체 Map을 `NotificationEvent.metadata`로 전달 → `NotificationService.convertMetadataToJson()`이 JSONB로 직렬화. `usage`, `model`은 payload 최상위 키이므로 별도 파싱 없이 자동 포함됨.
  - [x] 누락 시 `ClaudeWebhookHandler` 또는 관련 Service에 저장 로직 추가 — 이미 처리됨, 변경 없음
- [x] DB에서 metadata 컬럼 조회하여 실제 저장 확인
  > 코드 레벨 확인 완료: `Notification.metadata` (`@Column(columnDefinition = "jsonb")`)에 payload 전체가 저장되므로 `usage`, `model` 포함이 보장됨
  ```sql
  SELECT metadata FROM notifications WHERE source = 'CLAUDE' ORDER BY created_at DESC LIMIT 1;
  ```

---

## 최종 검증

- [ ] 정상 케이스 — `last_assistant_message` 포함
  ```bash
  echo '{"session_id":"test","transcript_path":"","model":"claude-sonnet-4-6","usage":{"input_tokens":1234,"output_tokens":567},"last_assistant_message":"테스트 작업이 완료되었습니다. 파일 3개를 수정했습니다."}' | bash .claude/hooks/send-stop-webhook.sh
  ```
- [ ] Fallback 케이스 — `last_assistant_message` 누락, `transcript_path` 사용
  ```bash
  echo '{"session_id":"test","transcript_path":"/tmp/test_transcript.jsonl","model":"claude-sonnet-4-6","usage":{"input_tokens":100,"output_tokens":50}}' | bash .claude/hooks/send-stop-webhook.sh
  ```
- [ ] 특수문자 케이스 — 따옴표, 백슬래시, 줄바꿈 포함 메시지 파싱 오류 없음 확인
- [ ] 토큰 0 케이스 — `token_line` 미표시 확인
  ```bash
  echo '{"session_id":"test","transcript_path":"","model":"claude-sonnet-4-6","usage":{"input_tokens":0,"output_tokens":0},"last_assistant_message":"작업 완료"}' | bash .claude/hooks/send-stop-webhook.sh
  ```
- [ ] HTTP 응답 200 확인 (curl 출력)
