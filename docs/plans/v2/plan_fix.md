# 라우팅 모드별 동작 개선 계획

## Context

사용자 의도:
- **IMMEDIATE**: 알림 원본 내용을 그대로 즉시 전달 (LLM 개입 없음)
- **DIGEST**: 설정한 기간 동안 쌓인 알림을 LLM으로 묶어 요약 후 전달. 해당 기간 내 새 알림이 없으면 전달하지 않음.

현재 코드의 문제:
- IMMEDIATE 전달이 LLM 요약(`summarize`) 완료 후에 실행되어 불필요한 지연 발생
- `buildMessage()`에 aiSummary 분기가 있지만, 실제로는 메모리 객체에 aiSummary가 반영되지 않아 데드 코드 상태
- DIGEST: 기간 내 알림이 없어도 LLM을 호출하고 빈 메시지를 전달하는 문제
- DIGEST 전달 실패 시 `nextRetryAt` 미설정으로 재시도 스케줄러가 처리 불가
- DIGEST 묶음 메시지 헤더가 단순하고 부정확 (소스 첫 번째 것만 사용, 우선순위 항상 MEDIUM)

---

## 개선 내용

### 개선 1: 파이프라인 분리 — 라우팅과 요약 독립 실행

**파일**: `backend/src/main/java/com/notio/notification/service/NotificationService.java`

현재 Branch B는 `summarize → route` 순차 실행으로 IMMEDIATE 전달이 LLM 완료까지 대기한다.
이를 두 개의 독립 비동기 작업으로 분리한다.

```java
// Branch B: 채널 라우팅 (LLM 대기 없이 즉시)
CompletableFuture.runAsync(() -> {
    try {
        channelRouter.route(saved);
    } catch (Exception e) {
        log.error("event=channel_routing_failed notification_id={} user_id={}",
            saved.getId(), saved.getUserId(), e);
    }
}, VIRTUAL_THREAD_EXECUTOR);

// Branch C: LLM 요약 (비동기 — DIGEST 묶음 처리 시 참고용으로 DB에 저장)
CompletableFuture.runAsync(() -> {
    try {
        notificationSummaryService.summarize(saved);
    } catch (Exception e) {
        log.warn("event=notification_summarize_failed notification_id={} user_id={}",
            saved.getId(), saved.getUserId(), e);
    }
}, VIRTUAL_THREAD_EXECUTOR);
```

기존 `try { summarize(saved); channelRouter.route(saved); }` 블록 전체를 위 두 블록으로 교체한다.

---

### 개선 2: IMMEDIATE — buildMessage() 데드 코드 제거

**파일**: `backend/src/main/java/com/notio/channel/ChannelRouter.java`

`summarize()`는 DB만 업데이트하고 메모리 객체는 수정하지 않으므로 aiSummary 분기는 항상 `false`다.
명시적으로 원본 body만 사용하도록 정리한다.

```java
// 변경 전
private ChannelMessage buildMessage(Notification notification) {
    String body = notification.getAiSummary() != null
        ? notification.getAiSummary()
        : notification.getBody();
    return new ChannelMessage(..., body, ...);
}

// 변경 후
private ChannelMessage buildMessage(Notification notification) {
    return new ChannelMessage(
        notification.getId(),
        notification.getTitle(),
        notification.getBody(),
        notification.getSource(),
        notification.getPriority(),
        notification.getExternalUrl(),
        notification.getCreatedAt()
    );
}
```

---

### 개선 3: DIGEST — 기간 내 알림이 없으면 전달 스킵

**파일**: `backend/src/main/java/com/notio/channel/NotificationDigestScheduler.java`

현재 `processDigestForChannel()`은 `notifications`가 비어 있어도 LLM을 호출하거나
빈 상태로 진행될 수 있는 구조다.
`pendingLogs`가 비어있으면 이미 early return하지만, 만약 로그는 있으나 실제 알림이 DB에서
조회되지 않는 경우(삭제된 알림 등)에는 `DEAD` 처리로 넘어간다.

추가로, 스케줄러가 `processDigests()`에서 채널 ID를 조회할 때 `DIGEST_PENDING` 상태이고
`nextRetryAt <= now`인 채널만 대상으로 하므로, **기간이 아직 끝나지 않은 채널은 자동으로 스킵**된다.
이 동작이 명확히 유지되도록 `processDigestForChannel()` 상단에 방어 조건을 명시한다.

```java
private void processDigestForChannel(Long channelId) {
    List<ChannelDeliveryLog> pendingLogs = deliveryLogRepository
        .findByChannelIdAndStatusAndNextRetryAtBefore(
            channelId, DeliveryStatus.DIGEST_PENDING, Instant.now());

    // 해당 기간 내 전달할 알림이 없으면 스킵
    if (pendingLogs.isEmpty()) {
        log.debug("event=digest_skipped_no_notifications channel_id={}", channelId);
        return;
    }
    // ... 이하 기존 로직
}
```

---

### 개선 4: DIGEST 전달 실패 시 nextRetryAt 설정

**파일**: `backend/src/main/java/com/notio/channel/NotificationDigestScheduler.java`

현재 RETRY 상태로 바꿀 때 `nextRetryAt`을 설정하지 않아 `ChannelDeliveryScheduler`의 재시도 쿼리
(`nextRetryAt <= now`)에 걸리지 않는다.

```java
// 변경 전
pendingLogs.forEach(l -> {
    l.setStatus(result.retryable() ? DeliveryStatus.RETRY : DeliveryStatus.DEAD);
    l.setLastError(result.errorMessage());
});

// 변경 후
Instant retryAt = Instant.now().plus(5, ChronoUnit.MINUTES);
pendingLogs.forEach(l -> {
    if (result.retryable()) {
        l.setStatus(DeliveryStatus.RETRY);
        l.setNextRetryAt(retryAt);
    } else {
        l.setStatus(DeliveryStatus.DEAD);
    }
    l.setLastError(result.errorMessage());
});
```

---

### 개선 5: DIGEST 묶음 메시지 헤더 개선

**파일**: `backend/src/main/java/com/notio/channel/NotificationDigestScheduler.java`

현재 제목, 소스, 우선순위가 부정확하다.

```java
// 변경 전
new ChannelMessage(
    notifications.get(0).getId(),
    "[묶음 알림] " + notifications.size() + "개",
    digestContent,
    notifications.get(0).getSource(),   // 첫 번째 소스만
    NotificationPriority.MEDIUM,        // 항상 MEDIUM
    null,
    Instant.now()
);

// 변경 후
String sourceSummary = notifications.stream()
    .map(n -> n.getSource().name())
    .distinct()
    .sorted()
    .collect(Collectors.joining(", "));

NotificationPriority maxPriority = notifications.stream()
    .map(Notification::getPriority)
    .max(Comparator.naturalOrder())
    .orElse(NotificationPriority.MEDIUM);

new ChannelMessage(
    notifications.get(0).getId(),
    "[묶음 알림] " + notifications.size() + "개 · " + sourceSummary,
    digestContent,
    notifications.get(0).getSource(),
    maxPriority,
    null,
    Instant.now()
);
```

---

## 수정 파일 요약

| 파일 | 변경 내용 |
|------|-----------|
| `notification/service/NotificationService.java` | Branch B → 라우팅(B)과 요약(C) 독립 비동기 분리 |
| `channel/ChannelRouter.java` | `buildMessage()` — aiSummary 데드 코드 제거, 원본 body 사용 |
| `channel/NotificationDigestScheduler.java` | 빈 알림 스킵 명시, 실패 시 `nextRetryAt` 설정, 메시지 헤더 개선 |

---

## 검증 방법

1. **IMMEDIATE 지연 개선 확인**: 알림 수신 후 Slack/Telegram에 도달하는 시간이 LLM 요약 시간(수십 초)을 기다리지 않고 즉시 전달되는지 확인
2. **IMMEDIATE 원본 내용 확인**: 전달된 메시지 body가 원본 body 그대로인지 확인 (aiSummary 아님)
3. **DIGEST 기간 내 알림 없음**: 설정 기간 동안 알림이 없으면 채널에 아무것도 전달되지 않는지 확인
4. **DIGEST 정상 동작**: 여러 알림 수신 후 설정 기간 만료 시 LLM 묶음 요약 메시지 수신 확인
5. **DIGEST 헤더**: 복수 소스(예: GITHUB + SLACK) 알림이 섞인 경우 제목에 소스 목록 노출 확인
6. **DIGEST 재시도**: 채널 전달 실패(retryable) 시 5분 후 `ChannelDeliveryScheduler`가 재처리하는지 확인
7. **로그**: `event=channel_routing_*`와 `event=notification_summarize_*`가 독립적으로(순서 무관하게) 찍히는지 확인
