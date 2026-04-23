#!/usr/bin/env bash
set -eu

INPUT="$(cat)"

BACKEND_URL="${NOTIO_BACKEND_WEBHOOK_URL:-http://172.28.6.247:8080/api/v1/webhook/claude}"
TOKEN="${NOTIO_WEBHOOK_CLAUDE_TOKEN:-ntio_wh_TJTjpX4cnPpe_iJFT209XqNxtzx9Kt2gosS40snSfnY0QzqyM_OWXc8g}"

PAYLOAD="$(python3 -c "
import json
import sys
from datetime import datetime, timezone

try:
    data = json.loads('''$INPUT''')

    # last_assistant_message 추출 (1800자까지)
    message = 'Claude Code 작업이 완료되었습니다.'

    external_id = f'claude-stop-{int(datetime.now(timezone.utc).timestamp())}'

    # API 스펙에 맞는 payload 생성
    payload = {
        'event_type': 'notification',
        'notification': {
            'id': external_id,
            'title': 'Claude Code 작업 완료',
            'message': message,
            'priority': 'medium',
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
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
