# Plan: send-stop-webhook.sh 개선 — 작업 내용 및 토큰 사용량 포함

## Context

현재 `send-stop-webhook.sh`는 StopHook이 실행될 때 항상 고정된 메시지("Claude Code 작업이 완료되었습니다.")만 전송한다. Claude Code StopHook의 stdin 페이로드에는 `last_assistant_message`, `usage.input_tokens`, `usage.output_tokens`, `model` 등의 데이터가 포함되어 있으나, 현재 스크립트가 이를 읽지 않고 있다.

추가로, 현재 스크립트는 `'''$INPUT'''` 방식으로 셸 변수를 Python 코드에 직접 삽입하는데, 입력에 따옴표나 특수문자가 포함되면 파싱이 실패할 수 있는 취약점이 있다.

## 목표

1. 실제 마지막 어시스턴트 메시지(작업 내용 요약)를 `message` 필드에 포함
2. 입력/출력 토큰 사용량을 payload에 포함 (백엔드가 metadata로 저장)
3. 사용된 모델 정보도 포함
4. Python stdin 파이프 방식으로 안전하게 JSON 파싱

---

## 수정 대상 파일

- `.claude/hooks/send-stop-webhook.sh`

---

## StopHook stdin 페이로드 구조

```json
{
  "session_id": "string",
  "transcript_path": "/path/to/session.jsonl",
  "model": "claude-sonnet-4-6",
  "usage": {
    "input_tokens": 12345,
    "output_tokens": 678
  },
  "last_assistant_message": "작업이 완료되었습니다. ..."
}
```

## 백엔드 webhook 페이로드 (변경 후)

```json
{
  "event_type": "notification",
  "notification": {
    "id": "claude-stop-1747123456",
    "title": "Claude Code 작업 완료",
    "message": "[마지막 응답 내용, 최대 800자]\n\n입력 12,345 토큰 / 출력 678 토큰",
    "priority": "medium",
    "timestamp": "2026-05-15T12:34:56+00:00"
  },
  "usage": {
    "input_tokens": 12345,
    "output_tokens": 678
  },
  "model": "claude-sonnet-4-6"
}
```

`usage`와 `model`은 백엔드에서 metadata JSONB 컬럼에 자동 저장됨.

---

## 구현 계획

### 1. Python stdin 파이프 방식으로 전환

**기존 (안전하지 않음):**
```bash
PAYLOAD="$(python3 -c "
...
data = json.loads('''$INPUT''')
...")"
```

**변경 후:**
```bash
PAYLOAD="$(echo "$INPUT" | python3 -c "
import json, sys
data = json.load(sys.stdin)
...")"
```

### 2. `last_assistant_message` 추출

우선순위:
1. `data.get('last_assistant_message', '')` — StopHook이 직접 제공하는 필드
2. 비어있을 경우 `transcript_path`의 JSONL 파일에서 마지막 assistant 메시지 텍스트 추출
3. 모두 실패 시 기본 메시지 사용

transcript 파싱 로직:
```python
transcript_path = data.get('transcript_path', '')
if transcript_path and os.path.exists(transcript_path):
    with open(transcript_path, 'r') as f:
        for line in f:
            try:
                entry = json.loads(line)
                if entry.get('role') == 'assistant':
                    content = entry.get('message', {}).get('content', [])
                    for block in content:
                        if block.get('type') == 'text':
                            last_text = block.get('text', '')
            except: pass
```

### 3. 메시지 포맷 구성

```python
# 작업 내용: 최대 800자
summary = (last_assistant_message[:800] + '...') if len(last_assistant_message) > 800 else last_assistant_message

# 토큰 사용량 라인 (0이면 생략)
token_line = ''
if input_tokens > 0 or output_tokens > 0:
    token_line = f'\n\n입력 {input_tokens:,} 토큰 / 출력 {output_tokens:,} 토큰'

message = (summary or 'Claude Code 작업이 완료되었습니다.') + token_line
```

### 4. payload에 usage, model 추가

```python
payload = {
    'event_type': 'notification',
    'notification': {
        'id': external_id,
        'title': 'Claude Code 작업 완료',
        'message': message,
        'priority': 'medium',
        'timestamp': datetime.now(timezone.utc).isoformat()
    },
    'usage': {
        'input_tokens': input_tokens,
        'output_tokens': output_tokens
    },
    'model': model
}
```

---

## 검증 방법

1. 스크립트를 직접 실행하여 테스트:
```bash
echo '{"session_id":"test","transcript_path":"","model":"claude-sonnet-4-6","usage":{"input_tokens":1234,"output_tokens":567},"last_assistant_message":"테스트 작업이 완료되었습니다. 파일 3개를 수정했습니다."}' | bash .claude/hooks/send-stop-webhook.sh
```

2. Claude Code 작업 완료 후 Notio 앱에서 알림 확인:
   - 메시지에 실제 작업 내용이 포함되는지
   - 토큰 사용량이 표시되는지
   - 백엔드 notification metadata에 `usage`, `model` 필드 저장 여부

3. `last_assistant_message`가 비어있는 경우 transcript 파싱 fallback 동작 확인:
```bash
echo '{"session_id":"test","transcript_path":"/tmp/test_transcript.jsonl","model":"claude-sonnet-4-6","usage":{"input_tokens":100,"output_tokens":50}}' | bash .claude/hooks/send-stop-webhook.sh
```
