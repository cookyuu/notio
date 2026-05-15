#!/usr/bin/env bash
set -eu

BACKEND_URL="${NOTIO_BACKEND_WEBHOOK_URL:-http://192.168.0.87:8080/api/v1/webhook/claude}"
TOKEN="${NOTIO_WEBHOOK_CLAUDE_TOKEN:-ntio_wh_TJTjpX4cnPpe_iJFT209XqNxtzx9Kt2gosS40snSfnY0QzqyM_OWXc8g}"

PAYLOAD="$(python3 -c "
import json
import os
import sys
from datetime import datetime, timezone

try:
    data = json.load(sys.stdin)

    DEFAULT_MESSAGE = 'Claude Code 작업이 완료되었습니다.'

    last_assistant_message = data.get('last_assistant_message', '')
    if not last_assistant_message:
        transcript_path = data.get('transcript_path', '')
        if transcript_path and os.path.exists(transcript_path):
            extracted = ''
            with open(transcript_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    try:
                        entry = json.loads(line)
                        if entry.get('role') == 'assistant':
                            content = entry.get('content', [])
                            for block in content:
                                if isinstance(block, dict) and block.get('type') == 'text':
                                    extracted = block.get('text', '')
                    except Exception:
                        continue
            if extracted:
                last_assistant_message = extracted

    usage = data.get('usage', {})
    input_tokens = usage.get('input_tokens', 0)
    output_tokens = usage.get('output_tokens', 0)
    model = data.get('model', '')

    summary = last_assistant_message[:800] + '...' if len(last_assistant_message) > 800 else last_assistant_message

    if input_tokens == 0 and output_tokens == 0:
        token_line = ''
    else:
        token_line = f'\n\n입력 {input_tokens:,} 토큰 / 출력 {output_tokens:,} 토큰'

    message = (summary or DEFAULT_MESSAGE) + token_line

    external_id = f'claude-stop-{int(datetime.now(timezone.utc).timestamp())}'

    payload = {
        'event_type': 'notification',
        'notification': {
            'id': external_id,
            'title': 'Claude Code 작업 완료',
            'message': message,
            'priority': 'medium',
            'timestamp': datetime.now(timezone.utc).isoformat()
        },
        'usage': {'input_tokens': input_tokens, 'output_tokens': output_tokens},
        'model': model,
    }

    print(json.dumps(payload))
except Exception as e:
    # Fallback: 기본 메시지 전송
    external_id = f'claude-stop-{int(datetime.now(timezone.utc).timestamp())}'
    payload = {
        'event_type': 'notification',
        'notification': {
            'id': external_id,
            'title': 'Claude Code 작업 완료',
            'message': 'Claude Code 작업이 완료되었습니다.',
            'priority': 'medium',
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
    }
    print(json.dumps(payload))
    sys.exit(0)
")"

curl -sS -X POST "$BACKEND_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "$PAYLOAD" || true
