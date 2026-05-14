package com.notio.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.channel.domain.ChannelDeliveryLog;
import com.notio.channel.domain.ChannelStatus;
import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.DeliveryStatus;
import com.notio.channel.domain.NotificationChannel;
import com.notio.channel.metrics.ChannelDeliveryMetrics;
import com.notio.channel.provider.ChannelDeliveryResult;
import com.notio.channel.provider.NotificationChannelProvider;
import com.notio.channel.repository.ChannelDeliveryLogRepository;
import com.notio.channel.repository.NotificationChannelRepository;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import com.notio.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDigestSchedulerTest {

    @Mock private ChannelDeliveryLogRepository deliveryLogRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationChannelRepository channelRepository;
    @Mock private ChannelProviderRegistry providerRegistry;
    @Mock private PromptBuilder promptBuilder;
    @Mock private LlmProvider llmProvider;
    @Mock private ChannelDeliveryMetrics metrics;

    private NotificationDigestScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificationDigestScheduler(
            deliveryLogRepository, notificationRepository, channelRepository,
            providerRegistry, promptBuilder, llmProvider, metrics
        );
    }

    @Test
    void processDigestsDeliversSuccessfullyAndUpdatesLogsToSuccess() {
        ChannelDeliveryLog log1 = pendingLog(1L, 1L);
        ChannelDeliveryLog log2 = pendingLog(2L, 1L);
        Notification n1 = notification(1L);
        Notification n2 = notification(2L);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log1, log2));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(n1, n2));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("묶음 요약 내용");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.success("ext-msg-id"));

        scheduler.processDigests();

        assertThat(log1.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(log2.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(log1.getExternalMessageId()).isEqualTo("ext-msg-id");
        verify(deliveryLogRepository).saveAll(anyList());
        verify(metrics).recordDigestDelivery(ChannelType.SLACK.name(), 2);
    }

    @Test
    void processDigestsUpdatesLogsToRetryOnRetryableFailure() {
        ChannelDeliveryLog log = pendingLog(1L, 1L);
        Notification notification = notification(1L);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(notification));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("summary");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.failure("rate limit", true));

        scheduler.processDigests();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.RETRY);
    }

    @Test
    void processDigestsUpdatesLogsToDeadOnNonRetryableFailure() {
        ChannelDeliveryLog log = pendingLog(1L, 1L);
        Notification notification = notification(1L);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(notification));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("summary");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.failure("bad webhook", false));

        scheduler.processDigests();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.DEAD);
    }

    @Test
    void processDigestsSetsLogsToDeadWhenNotificationsNotFound() {
        ChannelDeliveryLog log = pendingLog(1L, 1L);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log));
        when(notificationRepository.findAllById(any())).thenReturn(List.of());

        scheduler.processDigests();

        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.DEAD);
        verify(llmProvider, never()).chat(any());
    }

    @Test
    void processDigestsDoesNothingWhenNoExpiredPendingLogs() {
        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of());

        scheduler.processDigests();

        verify(notificationRepository, never()).findAllById(any());
        verify(llmProvider, never()).chat(any());
    }

    // -----------------------------------------------------------------------
    // Phase 6 — DIGEST 빈 알림 스킵: pendingLogs가 비어있으면 채널 전달 없이 early return
    // -----------------------------------------------------------------------

    @Test
    void processDigestsSkipsChannelDeliveryWhenPendingLogsAreEmpty() {
        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of());

        scheduler.processDigests();

        verify(notificationRepository, never()).findAllById(any());
        verify(llmProvider, never()).chat(any());
        verify(providerRegistry, never()).get(any());
        verify(deliveryLogRepository, never()).saveAll(anyList());
    }

    // -----------------------------------------------------------------------
    // Phase 6 — DIGEST 정상 동작: 여러 알림 수신 후 LLM 묶음 요약 메시지 전달
    // -----------------------------------------------------------------------

    @Test
    void processDigestsDeliversDigestMessageWithLlmSummaryForMultipleNotifications() {
        ChannelDeliveryLog log1 = pendingLog(1L, 1L);
        ChannelDeliveryLog log2 = pendingLog(2L, 1L);
        ChannelDeliveryLog log3 = pendingLog(3L, 1L);
        Notification n1 = notification(1L, NotificationSource.GITHUB, NotificationPriority.HIGH);
        Notification n2 = notification(2L, NotificationSource.GITHUB, NotificationPriority.MEDIUM);
        Notification n3 = notification(3L, NotificationSource.GITHUB, NotificationPriority.LOW);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log1, log2, log3));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(n1, n2, n3));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("3개 알림 묶음 요약");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.success("ext-digest-id"));

        scheduler.processDigests();

        assertThat(log1.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(log2.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(log3.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        verify(llmProvider).chat(any());
        verify(metrics).recordDigestDelivery(ChannelType.SLACK.name(), 3);

        ArgumentCaptor<com.notio.channel.provider.ChannelMessage> messageCaptor =
            ArgumentCaptor.forClass(com.notio.channel.provider.ChannelMessage.class);
        verify(provider).deliver(any(), messageCaptor.capture());
        assertThat(messageCaptor.getValue().body()).isEqualTo("3개 알림 묶음 요약");
    }

    // -----------------------------------------------------------------------
    // Phase 6 — DIGEST 헤더: 복수 소스(GITHUB + SLACK) 혼재 시 제목에 소스 목록 노출
    // -----------------------------------------------------------------------

    @Test
    void processDigestsTitleContainsDistinctSourcesSortedWhenMultipleSourcesPresent() {
        ChannelDeliveryLog log1 = pendingLog(1L, 1L);
        ChannelDeliveryLog log2 = pendingLog(2L, 1L);
        Notification n1 = notification(1L, NotificationSource.SLACK, NotificationPriority.MEDIUM);
        Notification n2 = notification(2L, NotificationSource.GITHUB, NotificationPriority.HIGH);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log1, log2));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(n1, n2));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("묶음 요약");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.success("ext-id"));

        scheduler.processDigests();

        ArgumentCaptor<com.notio.channel.provider.ChannelMessage> messageCaptor =
            ArgumentCaptor.forClass(com.notio.channel.provider.ChannelMessage.class);
        verify(provider).deliver(any(), messageCaptor.capture());

        String title = messageCaptor.getValue().title();
        // "[묶음 알림] 2개 · GITHUB, SLACK" 형태: source는 알파벳 오름차순 정렬
        assertThat(title)
            .as("DIGEST 제목은 '[묶음 알림] N개 · SOURCE1, SOURCE2' 형식이어야 한다")
            .isEqualTo("[묶음 알림] 2개 · GITHUB, SLACK");
    }

    @Test
    void processDigestsTitleContainsSingleSourceWhenAllNotificationsHaveSameSource() {
        ChannelDeliveryLog log1 = pendingLog(1L, 1L);
        ChannelDeliveryLog log2 = pendingLog(2L, 1L);
        Notification n1 = notification(1L, NotificationSource.GITHUB, NotificationPriority.HIGH);
        Notification n2 = notification(2L, NotificationSource.GITHUB, NotificationPriority.MEDIUM);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log1, log2));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(n1, n2));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("묶음 요약");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.success("ext-id"));

        scheduler.processDigests();

        ArgumentCaptor<com.notio.channel.provider.ChannelMessage> messageCaptor =
            ArgumentCaptor.forClass(com.notio.channel.provider.ChannelMessage.class);
        verify(provider).deliver(any(), messageCaptor.capture());
        assertThat(messageCaptor.getValue().title()).isEqualTo("[묶음 알림] 2개 · GITHUB");
    }

    // -----------------------------------------------------------------------
    // Phase 6 — DIGEST 재시도: retryable=true 실패 시 nextRetryAt이 5분 후로 설정되고
    // status=RETRY, retryable=false면 status=DEAD
    // -----------------------------------------------------------------------

    @Test
    void processDigestsSetsNextRetryAtApproximatelyFiveMinutesLaterOnRetryableFailure() {
        ChannelDeliveryLog log = pendingLog(1L, 1L);
        Notification notification = notification(1L, NotificationSource.GITHUB, NotificationPriority.HIGH);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(notification));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("summary");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.failure("rate limit", true));

        Instant beforeCall = Instant.now();
        scheduler.processDigests();
        Instant afterCall = Instant.now();

        assertThat(log.getStatus())
            .as("retryable 실패 시 status는 RETRY여야 한다")
            .isEqualTo(DeliveryStatus.RETRY);
        assertThat(log.getNextRetryAt())
            .as("nextRetryAt이 설정되어야 한다")
            .isNotNull();
        assertThat(log.getNextRetryAt())
            .as("nextRetryAt은 현재 시각으로부터 약 5분 후여야 한다")
            .isBetween(
                beforeCall.plusSeconds(4 * 60 + 55),
                afterCall.plusSeconds(5 * 60 + 5)
            );
        assertThat(log.getLastError())
            .as("lastError가 설정되어야 한다")
            .isEqualTo("rate limit");
    }

    @Test
    void processDigestsSetsStatusToDeadOnNonRetryableFailureAndSetsLastError() {
        ChannelDeliveryLog log = pendingLog(1L, 1L);
        Notification notification = notification(1L, NotificationSource.GITHUB, NotificationPriority.HIGH);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(notification));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("summary");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.failure("invalid webhook url", false));

        scheduler.processDigests();

        assertThat(log.getStatus())
            .as("non-retryable 실패 시 status는 DEAD여야 한다")
            .isEqualTo(DeliveryStatus.DEAD);
        assertThat(log.getLastError())
            .as("lastError가 설정되어야 한다")
            .isEqualTo("invalid webhook url");
    }

    // -----------------------------------------------------------------------
    // Phase 6 — DIGEST 재시도: ChannelDeliveryScheduler가 status=RETRY인 로그를
    // 5분 후 재처리한다는 것을 계약 수준에서 검증 (nextRetryAt이 미래로 설정되면
    // findTop50ByStatusAndNextRetryAtBefore 쿼리에서 제외됨을 기반으로 함)
    // -----------------------------------------------------------------------

    @Test
    void retryableDigestFailureResultsInNextRetryAtSetInFutureSoSchedulerPicksItUpLater() {
        ChannelDeliveryLog log = pendingLog(1L, 1L);
        Notification notification = notification(1L, NotificationSource.GITHUB, NotificationPriority.HIGH);
        NotificationChannel channel = activeChannel();
        NotificationChannelProvider provider = org.mockito.Mockito.mock(NotificationChannelProvider.class);

        when(deliveryLogRepository.findDistinctChannelIdsByStatusAndNextRetryAtBefore(
            eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(1L));
        when(deliveryLogRepository.findByChannelIdAndStatusAndNextRetryAtBefore(
            eq(1L), eq(DeliveryStatus.DIGEST_PENDING), any(Instant.class))).thenReturn(List.of(log));
        when(notificationRepository.findAllById(any())).thenReturn(List.of(notification));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));
        when(promptBuilder.buildDigestSummaryPrompt(any())).thenReturn(new LlmPrompt("sys", "usr"));
        when(llmProvider.chat(any())).thenReturn("summary");
        when(providerRegistry.get(ChannelType.SLACK)).thenReturn(provider);
        when(provider.deliver(any(), any())).thenReturn(ChannelDeliveryResult.failure("timeout", true));

        scheduler.processDigests();

        // ChannelDeliveryScheduler는 status=RETRY && nextRetryAt < now() 조건으로 조회
        // nextRetryAt이 미래(약 5분 후)로 설정되면 즉시 재처리되지 않고
        // 5분 후 ChannelDeliveryScheduler.processRetries()에서 처리됨
        assertThat(log.getStatus()).isEqualTo(DeliveryStatus.RETRY);
        assertThat(log.getNextRetryAt())
            .as("nextRetryAt이 현재보다 미래여야 ChannelDeliveryScheduler가 5분 뒤 처리한다")
            .isAfter(Instant.now());
    }

    private ChannelDeliveryLog pendingLog(Long notificationId, Long channelId) {
        return ChannelDeliveryLog.builder()
            .notificationId(notificationId).channelId(channelId)
            .status(DeliveryStatus.DIGEST_PENDING)
            .nextRetryAt(Instant.now().minusSeconds(60)).build();
    }

    private Notification notification(Long id) {
        return Notification.builder()
            .id(id).userId(1L).source(NotificationSource.GITHUB)
            .title("Test " + id).body("Body " + id).priority(NotificationPriority.HIGH).build();
    }

    private Notification notification(Long id, NotificationSource source, NotificationPriority priority) {
        return Notification.builder()
            .id(id).userId(1L).source(source)
            .title("Test " + id).body("Body " + id).priority(priority).build();
    }

    private NotificationChannel activeChannel() {
        return NotificationChannel.builder()
            .id(1L).userId(1L).channelType(ChannelType.SLACK)
            .displayName("test").credentialEncrypted("enc")
            .status(ChannelStatus.ACTIVE).build();
    }
}
