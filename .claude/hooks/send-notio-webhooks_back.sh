set -eu

INPUT="$(cat)"

BACKEND_URL="${NOTIO_BACKEND_WEBHOOK_URL:-http://172.28.6.247:8080/api/v1/webhook/claude}"
TOKEN="${NOTIO_WEBHOOK_CLAUDE_TOKEN:-ntio_wh_TJTjpX4cnPpe_iJFT209XqNxtzx9Kt2gosS40snSfnY0QzqyM_OWXc8g}"

# jq가 있으면 사용하고, 없으면 Python으로 fallback
if command -v jq >/dev/null 2>&1; then
  MESSAGE="$(printf '%s' "$INPUT" | jq -r '.last_assistant_message // "Claude Code 작업이 완료되었습니다."' | head -c 1800)"
  PAYLOAD="$(jq -n \
    --arg title "Claude Code 작업 완료" \
    --arg message "$MESSAGE" \
    --arg priority "medium" \
    --arg external_id "$(date +%s)" \
    '{
      title: $title,
      message: $message,
      priority: $priority,
      external_id: $external_id
    }')"
else
  # Python fallback
  PAYLOAD="$(python3 -c "
import json
import sys
from datetime import datetime

try:
    data = json.loads('''$INPUT''')
    message = data.get('last_assistant_message', 'Claude Code 작업이 완료되었습니다.')[:1800]
except:
    message = 'Claude Code 작업이 완료되었습니다.'

payload = {
    'title': 'Claude Code 작업 완료',
    'message': message,
    'priority': 'medium',
    'external_id': str(int(datetime.now().timestamp()))
}
print(json.dumps(payload))
")"
fi

curl -sS -X POST "$BACKEND_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "$PAYLOAD"
