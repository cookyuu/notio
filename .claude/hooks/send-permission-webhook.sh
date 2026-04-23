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

    # 필드 추출
    tool_name = data.get('tool_name', 'Unknown')
    tool_input = data.get('tool_input', {})

    # 도구별 메시지 생성
    if tool_name == 'Bash':
        command = tool_input.get('command', '')[:50]
        detail = f'명령어: {command}'
    elif tool_name in ['Edit', 'Write']:
        file_path = tool_input.get('file_path', '')
        file_name = file_path.split('/')[-1] if file_path else 'unknown'
        detail = f'파일: {file_name}'
    elif tool_name == 'Read':
        file_path = tool_input.get('file_path', '')
        file_name = file_path.split('/')[-1] if file_path else 'unknown'
        detail = f'파일: {file_name}'
    else:
        detail = tool_input.get('description', '')[:50] or tool_name

    message = '권한 필요: {tool_name}'
    external_id = f'claude-perm-{int(datetime.now(timezone.utc).timestamp())}'

    payload = {
        'event_type': 'notification',
        'notification': {
            'id': external_id,
            'title': 'Claude Code 권한 승인 필요',
            'message': message,
            'priority': 'high',
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
    }

    print(json.dumps(payload))
except Exception as e:
    # Fallback: 기본 메시지 전송
    external_id = f'claude-perm-{int(datetime.now(timezone.utc).timestamp())}'
    payload = {
        'event_type': 'notification',
        'notification': {
            'id': external_id,
            'title': 'Claude Code 권한 승인 필요',
            'message': 'Claude Code에서 권한 승인이 필요합니다.',
            'priority': 'high',
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
