# Connection API Key 보안 정책

## 원문 API Key 저장 금지

### 정책

**원문 API Key는 절대로 저장하지 않습니다.**

### 저장 금지 위치

- ❌ 로컬 DB (Drift/SQLite)
- ❌ Secure Storage (flutter_secure_storage)
- ❌ Shared Preferences
- ❌ 메모리 캐시 (Provider state)
- ❌ 로그 출력

### 원문 API Key 노출 시점

원문 API Key는 다음 두 경우에만 1회 반환됩니다:

1. **Connection 생성 (API Key 방식)**
   - `POST /api/v1/connections`
   - Response: `{ connection: {...}, api_key: "ntio_wh_..." }`

2. **API Key 재발급**
   - `POST /api/v1/connections/{id}/rotate-key`
   - Response: `{ connection: {...}, api_key: "ntio_wh_..." }`

### 허용되는 사용

✅ **일회성 화면 표시 및 복사**
- OneTimeApiKeyDialog에서 1회 표시
- 사용자가 클립보드에 복사
- Dialog 닫기 시 즉시 폐기

### 구현 규칙

```dart
// ❌ 절대 금지
final apiKey = response.apiKey;
await secureStorage.write(key: 'api_key', value: apiKey);
await preferences.setString('api_key', apiKey);
await database.insert('api_keys', {'value': apiKey});

// ✅ 허용
showDialog(
  context: context,
  builder: (context) => OneTimeApiKeyDialog(
    apiKey: response.apiKey, // Dialog state에만 보관
  ),
);
```

### Dialog 닫기 후 재조회 불가

- 사용자가 dialog를 닫으면 원문 key는 즉시 폐기됩니다.
- 화면 재진입, 앱 재시작 후 원문 key를 다시 볼 수 없습니다.
- key_preview만 표시됩니다: `ntio_wh_ab12cd34_********`

### API 응답 계약

**목록 조회** (`GET /api/v1/connections`)
```json
{
  "id": 13,
  "key_preview": "ntio_wh_ab12cd34_********",
  // api_key 필드 없음
}
```

**상세 조회** (`GET /api/v1/connections/{id}`)
```json
{
  "id": 13,
  "key_preview": "ntio_wh_ab12cd34_********",
  // api_key 필드 없음
}
```

**생성 응답** (`POST /api/v1/connections`) - API Key 방식만
```json
{
  "connection": {
    "id": 13,
    "key_preview": "ntio_wh_ab12cd34_********"
  },
  "api_key": "ntio_wh_ab12cd34_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
}
```

**rotate 응답** (`POST /api/v1/connections/{id}/rotate-key`)
```json
{
  "connection": {
    "id": 13,
    "key_preview": "ntio_wh_cd34ef56_********"
  },
  "api_key": "ntio_wh_cd34ef56_yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy"
}
```

### 페이지네이션 응답

기존 `ApiResponse<T>` 구조를 재사용합니다:

```dart
class ApiResponse<T> {
  final bool success;
  final T? data;
  final ErrorResponse? error;
}

class PagedResponse<T> {
  final List<T> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool first;
  final bool last;
}
```

목록 조회:
```dart
ApiResponse<PagedResponse<ConnectionModel>>
```

상세 조회:
```dart
ApiResponse<ConnectionModel>
```

생성:
```dart
ApiResponse<ConnectionCreateResponse>
```

### 타입 레벨 보장

ConnectionModel은 api_key 필드가 없습니다.
ConnectionCreateResponse와 ConnectionRotateKeyResponse만 api_key 필드를 가집니다.

```dart
class ConnectionModel {
  // api_key 필드 없음
  final String? keyPreview;
}

class ConnectionCreateResponse {
  final ConnectionModel connection;
  final String? apiKey; // OAuth는 null
}

class ConnectionRotateKeyResponse {
  final ConnectionModel connection;
  final String apiKey; // non-nullable
}
```

### 요약

1. 원문 API Key는 생성/rotate 응답에서만 1회 반환
2. Dialog state에만 임시 보관
3. 사용자가 복사 후 dialog 닫기
4. 절대 저장하지 않음
5. key_preview만 목록/상세에서 표시
