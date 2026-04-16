# Notio Fix API 명세서

**버전**: v2.0-auth-fix  
**Base URL**: `http://localhost:8080`  
**API Version**: `/api/v1`  
**최종 수정일**: 2026-04-16

---

## 중요 사항

이 문서는 `docs/plans/plan_fix.md` 기준의 인증 확장 작업을 위한 API 명세서다.

기존 로그인/리프레시/로그아웃 중심 인증 구조를 다음 범위로 확장한다.

- 이메일 기반 회원가입
- 아이디 찾기
- 비밀번호 찾기 요청/확정
- 향후 Google, Apple, Kakao, Naver OAuth 확장을 위한 공통 API

핵심 원칙은 다음과 같다.

- 로그인 식별자는 `이메일 = 아이디`로 본다.
- 아이디 찾기는 계정 존재 여부를 노출하지 않는 가입 이메일 안내형 기능으로 정의한다.
- 비밀번호 찾기는 이메일 링크 기반 재설정으로 정의한다.
- OAuth는 provider adapter + registry 기반 확장 구조를 전제로 한다.

---

## 목차

1. [공통 사항](#1-공통-사항)
2. [인증 공통 타입](#2-인증-공통-타입)
3. [로컬 인증 API](#3-로컬-인증-api)
4. [OAuth 공통 API](#4-oauth-공통-api)
5. [에러 코드](#5-에러-코드)
6. [Rate Limit 및 보안 정책](#6-rate-limit-및-보안-정책)

---

## 1. 공통 사항

### 1.1 공통 응답 형식

성공 응답:

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

실패 응답:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

### 1.2 상태 코드

| 상태 코드 | 설명 |
|-----------|------|
| `200 OK` | 일반 성공 응답 |
| `201 Created` | 신규 리소스 성격의 생성 성공 |
| `302 Found` | OAuth callback redirect |
| `400 Bad Request` | 요청 형식 또는 값 검증 실패 |
| `401 Unauthorized` | 인증 실패 또는 토큰 오류 |
| `403 Forbidden` | 접근 권한 없음 |
| `404 Not Found` | 리소스 없음 |
| `409 Conflict` | 중복 또는 상태 충돌 |
| `429 Too Many Requests` | Rate limit 초과 |
| `500 Internal Server Error` | 서버 내부 오류 |

### 1.3 필드 명명 규칙

- JSON 필드는 `snake_case`를 사용한다.
- enum 문자열은 `UPPER_SNAKE_CASE`를 사용한다.
- 날짜/시간은 ISO 8601 UTC 기준 문자열을 사용한다.

### 1.4 토큰 응답 공통 구조

인증 성공 시 사용하는 세션 응답 구조는 다음을 기준으로 한다.

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `access_token` | string | Y | JWT access token |
| `refresh_token` | string | Y | JWT refresh token |
| `token_type` | string | Y | 기본값 `Bearer` |
| `expires_in` | number | Y | access token 만료 시간(초) |
| `user` | object | Y | 현재 사용자 요약 정보 |

`user` 객체:

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | number | Y | 사용자 ID |
| `primary_email` | string | Y | 기본 이메일 |
| `display_name` | string | N | 표시명 |
| `status` | string | Y | 사용자 상태 |

---

## 2. 인증 공통 타입

### 2.1 AuthProvider

| 값 | 설명 |
|----|------|
| `LOCAL` | 이메일/비밀번호 |
| `GOOGLE` | Google OAuth |
| `APPLE` | Apple Sign In |
| `KAKAO` | Kakao OAuth |
| `NAVER` | Naver OAuth |

### 2.2 AuthPlatform

| 값 | 설명 |
|----|------|
| `WEB` | 웹 브라우저 |
| `MOBILE` | 모바일 앱 |

### 2.3 OAuthNormalizedProfile

OAuth provider 연동 시 서버 내부에서 정규화하는 사용자 프로필 구조:

| 필드 | 타입 | 설명 |
|------|------|------|
| `provider` | string | provider 식별자 |
| `provider_user_id` | string | provider 내부 사용자 식별자 |
| `email` | string | provider가 제공한 이메일 |
| `email_verified` | boolean | 이메일 검증 여부 |
| `display_name` | string | provider가 제공한 표시명 |

---

## 3. 로컬 인증 API

### 3.1 회원가입

**Endpoint**

```http
POST /api/v1/auth/signup
```

**설명**

- 이메일과 비밀번호로 계정을 생성한다.
- 내부적으로 `User + LOCAL AuthIdentity`를 생성한다.
- 중복 이메일은 허용하지 않는다.

**Request Body**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `email` | string | Y | 이메일 형식 | 로그인 이메일 |
| `password` | string | Y | 최소 8자 | 로그인 비밀번호 |

**예시 요청**

```json
{
  "email": "user@example.com",
  "password": "password1234"
}
```

**성공 응답**

상태 코드: `201 Created`

```json
{
  "success": true,
  "data": {
    "message": "회원가입이 완료되었습니다."
  },
  "error": null
}
```

**에러 케이스**

| 상태 코드 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `400` | `INVALID_INPUT_VALUE` | 이메일/비밀번호 형식 오류 |
| `409` | `EMAIL_ALREADY_EXISTS` | 이미 가입된 이메일 |

### 3.2 로그인

**Endpoint**

```http
POST /api/v1/auth/login
```

**설명**

- LOCAL identity 기준으로 로그인한다.
- 성공 시 access/refresh token을 발급한다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | string | Y | 로그인 이메일 |
| `password` | string | Y | 로그인 비밀번호 |

**예시 요청**

```json
{
  "email": "user@example.com",
  "password": "password1234"
}
```

**성공 응답**

상태 코드: `200 OK`

```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOi...",
    "refresh_token": "eyJhbGciOi...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "user": {
      "id": 1,
      "primary_email": "user@example.com",
      "display_name": null,
      "status": "ACTIVE"
    }
  },
  "error": null
}
```

### 3.3 아이디 찾기

**Endpoint**

```http
POST /api/v1/auth/find-id
```

**설명**

- 입력한 이메일을 기준으로 가입 안내 메일을 발송한다.
- 계정 존재 여부와 무관하게 동일한 성공 응답을 반환한다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | string | Y | 안내를 받을 이메일 |

**예시 요청**

```json
{
  "email": "user@example.com"
}
```

**성공 응답**

상태 코드: `200 OK`

```json
{
  "success": true,
  "data": {
    "message": "입력한 이메일로 가입 정보 안내를 전송했습니다."
  },
  "error": null
}
```

**보안 정책**

- 존재하는 계정이어도, 존재하지 않는 계정이어도 같은 응답을 반환한다.
- 응답 본문에 가입 여부를 직접 노출하지 않는다.

### 3.4 비밀번호 재설정 요청

**Endpoint**

```http
POST /api/v1/auth/password-reset/request
```

**설명**

- 비밀번호 재설정 메일을 요청한다.
- 계정 존재 여부와 무관하게 동일한 성공 응답을 반환한다.
- 서버는 raw token을 생성해 메일 링크에 담고, DB에는 `token_hash`만 저장한다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | string | Y | 재설정 메일 수신 이메일 |

**예시 요청**

```json
{
  "email": "user@example.com"
}
```

**성공 응답**

상태 코드: `200 OK`

```json
{
  "success": true,
  "data": {
    "message": "비밀번호 재설정 안내를 전송했습니다."
  },
  "error": null
}
```

### 3.5 비밀번호 재설정 확정

**Endpoint**

```http
POST /api/v1/auth/password-reset/confirm
```

**설명**

- reset token과 새 비밀번호를 받아 비밀번호를 변경한다.
- 성공 시 사용된 token을 소진 처리하고, 기존 refresh token을 모두 revoke한다.

**Request Body**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `token` | string | Y | - | 메일 링크에 포함된 raw token |
| `new_password` | string | Y | 최소 8자 | 새 비밀번호 |

**예시 요청**

```json
{
  "token": "raw-reset-token",
  "new_password": "newPassword1234"
}
```

**성공 응답**

상태 코드: `200 OK`

```json
{
  "success": true,
  "data": {
    "message": "비밀번호가 재설정되었습니다."
  },
  "error": null
}
```

**에러 케이스**

| 상태 코드 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `400` | `PASSWORD_RESET_TOKEN_INVALID` | 잘못된 token |
| `400` | `PASSWORD_RESET_TOKEN_EXPIRED` | 만료된 token |
| `400` | `INVALID_INPUT_VALUE` | 새 비밀번호 형식 오류 |

---

## 4. OAuth 공통 API

### 4.1 OAuth 시작

**Endpoint**

```http
POST /api/v1/auth/oauth/start
```

**설명**

- provider 인증 페이지로 이동하기 위한 URL과 state를 발급한다.
- `redirect_uri`와 `platform` 정보를 저장해 callback 시 검증한다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `provider` | string | Y | `GOOGLE`, `APPLE`, `KAKAO`, `NAVER` |
| `platform` | string | Y | `WEB`, `MOBILE` |
| `redirect_uri` | string | Y | callback 완료 후 돌아올 프론트 주소 |

**예시 요청**

```json
{
  "provider": "GOOGLE",
  "platform": "WEB",
  "redirect_uri": "https://app.notio.dev/auth/oauth/callback"
}
```

**성공 응답**

상태 코드: `200 OK`

```json
{
  "success": true,
  "data": {
    "authorization_url": "https://accounts.google.com/o/oauth2/v2/auth?...",
    "state": "oauth-state-value"
  },
  "error": null
}
```

### 4.2 OAuth Callback

**Endpoint**

```http
GET /api/v1/auth/oauth/callback/{provider}
```

**설명**

- provider callback을 수신한다.
- 서버는 `state`를 검증한 뒤 프론트 callback route로 redirect한다.
- 이 엔드포인트는 일반 JSON 응답 대신 redirect 응답을 사용한다.

**Path Parameter**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `provider` | string | Y | `GOOGLE`, `APPLE`, `KAKAO`, `NAVER` |

**Query Parameter**

| 이름 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `code` | string | N | provider authorization code |
| `state` | string | N | 요청 시 저장된 state |
| `error` | string | N | provider가 반환한 오류 코드 |

**성공 동작**

- 상태 코드: `302 Found`
- 대상: 프론트 `/auth/oauth/callback` 또는 플랫폼 redirect URI
- 전달 파라미터 예시:
  - `provider`
  - `code`
  - `state`
  - `error`

**에러 케이스**

| 상태 코드 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `400` | `OAUTH_STATE_INVALID` | state 검증 실패 |
| `400` | `OAUTH_CALLBACK_FAILED` | provider callback 오류 |
| `400` | `AUTH_PROVIDER_UNSUPPORTED` | 미지원 provider |

### 4.3 OAuth 교환

**Endpoint**

```http
POST /api/v1/auth/oauth/exchange
```

**설명**

- 프론트 callback 화면 또는 모바일 앱이 provider 결과를 서버에 전달한다.
- 서버는 provider code를 access token으로 교환하고, 사용자 프로필을 정규화한 뒤 앱 세션 토큰을 발급한다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `provider` | string | Y | `GOOGLE`, `APPLE`, `KAKAO`, `NAVER` |
| `platform` | string | Y | `WEB`, `MOBILE` |
| `code` | string | Y | provider authorization code |
| `state` | string | Y | callback state |
| `redirect_uri` | string | Y | 시작 시 사용한 redirect URI |

**예시 요청**

```json
{
  "provider": "GOOGLE",
  "platform": "WEB",
  "code": "provider-auth-code",
  "state": "oauth-state-value",
  "redirect_uri": "https://app.notio.dev/auth/oauth/callback"
}
```

**성공 응답**

상태 코드: `200 OK`

```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOi...",
    "refresh_token": "eyJhbGciOi...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "user": {
      "id": 1,
      "primary_email": "user@example.com",
      "display_name": "Notio User",
      "status": "ACTIVE"
    }
  },
  "error": null
}
```

**에러 케이스**

| 상태 코드 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `400` | `AUTH_PROVIDER_UNSUPPORTED` | 미지원 provider |
| `400` | `OAUTH_STATE_INVALID` | state 검증 실패 |
| `400` | `OAUTH_CALLBACK_FAILED` | code 교환 또는 프로필 조회 실패 |
| `400` | `AUTH_PROVIDER_EMAIL_REQUIRED` | provider 정책상 이메일이 필요하지만 누락된 경우 |

---

## 5. 에러 코드

| 에러 코드 | 설명 |
|-----------|------|
| `INVALID_INPUT_VALUE` | 요청 값 형식이 잘못된 경우 |
| `INVALID_CREDENTIALS` | 로그인 자격 증명 오류 |
| `UNAUTHORIZED` | 인증되지 않은 요청 |
| `EMAIL_ALREADY_EXISTS` | 이미 가입된 이메일 |
| `PASSWORD_RESET_TOKEN_INVALID` | 비밀번호 재설정 token 불일치 |
| `PASSWORD_RESET_TOKEN_EXPIRED` | 비밀번호 재설정 token 만료 |
| `AUTH_PROVIDER_UNSUPPORTED` | 지원하지 않는 OAuth provider |
| `AUTH_PROVIDER_EMAIL_REQUIRED` | provider 연동에 필요한 이메일이 없음 |
| `OAUTH_STATE_INVALID` | OAuth state 검증 실패 |
| `OAUTH_CALLBACK_FAILED` | OAuth callback 또는 code 교환 실패 |
| `RATE_LIMIT_EXCEEDED` | 허용 요청 수 초과 |
| `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

## 6. Rate Limit 및 보안 정책

### 6.1 Rate Limit 대상

다음 엔드포인트는 별도 제한 정책을 적용한다.

- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/find-id`
- `POST /api/v1/auth/password-reset/request`
- `POST /api/v1/auth/password-reset/confirm`
- `POST /api/v1/auth/oauth/start`
- `POST /api/v1/auth/oauth/exchange`

초과 시 응답:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "요청 횟수를 초과했습니다."
  }
}
```

### 6.2 계정 존재 여부 비노출

다음 API는 계정 존재 여부와 무관하게 동일 응답을 반환한다.

- `POST /api/v1/auth/find-id`
- `POST /api/v1/auth/password-reset/request`

### 6.3 비밀번호 재설정 보안 정책

- reset token은 raw 값 그대로 저장하지 않는다.
- DB에는 `token_hash`만 저장한다.
- token은 1회만 사용할 수 있다.
- 비밀번호 재설정 성공 시 기존 refresh token은 모두 revoke한다.

### 6.4 OAuth 보안 정책

- `state`는 반드시 서버 저장소와 대조 검증한다.
- callback 처리 시 `redirect_uri`와 `platform`을 함께 검증한다.
- provider access token, id token, raw reset token은 로그에 남기지 않는다.
