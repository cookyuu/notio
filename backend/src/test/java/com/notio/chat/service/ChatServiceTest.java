package com.notio.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.ai.rag.RagDocument;
import com.notio.ai.rag.RagRetriever;
import com.notio.ai.rag.TimeRange;
import com.notio.chat.domain.ChatMessage;
import com.notio.chat.domain.ChatMessageRole;
import com.notio.chat.dto.ChatMessageResponse;
import com.notio.chat.dto.ChatRequest;
import com.notio.chat.metrics.ChatMetrics;
import com.notio.chat.repository.ChatMessageRepository;
import com.notio.common.config.JacksonConfig;
import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.metrics.NotioMetrics;
import com.notio.common.metrics.NotioMetricsTagPolicy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class ChatServiceTest {

    @Test
    void chatUsesRagPromptAndLlmThenStoresAssistantResponse() {
        final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        final RagRetriever ragRetriever = mock(RagRetriever.class);
        final ChatTimeRangeExtractor timeRangeExtractor = mock(ChatTimeRangeExtractor.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final ChatService chatService = new ChatService(
                chatMessageRepository,
                ragRetriever,
                timeRangeExtractor,
                promptBuilder,
                llmProvider,
                aiProperties(),
                objectMapper(),
                chatMetrics()
        );
        final ChatMessage userMessage = message(
                1L,
                ChatMessageRole.USER,
                "오늘 중요한 알림 알려줘",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        final ChatMessage assistantMessage = message(
                2L,
                ChatMessageRole.ASSISTANT,
                "GitHub PR 리뷰 요청이 중요합니다.",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 1, 0, ZoneOffset.UTC)
        );
        final RagDocument document = new RagDocument(
                100L,
                "GITHUB",
                "PR review requested",
                "리뷰 요청",
                "HIGH",
                Instant.parse("2026-04-22T09:59:00Z"),
                0.91
        );
        final TimeRange timeRange = new TimeRange(
                Instant.parse("2026-04-21T15:00:00Z"),
                Instant.parse("2026-04-22T15:00:00Z")
        );
        final LlmPrompt prompt = new LlmPrompt("system", "user");
        final ArgumentCaptor<ChatMessage> savedMessageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(userMessage)
                .thenReturn(assistantMessage);
        when(timeRangeExtractor.extract("오늘 중요한 알림 알려줘")).thenReturn(Optional.of(timeRange));
        when(ragRetriever.retrieve(1L, "오늘 중요한 알림 알려줘", Optional.of(timeRange))).thenReturn(List.of(document));
        when(chatMessageRepository.findRecentByUserId(eq(1L), any(Pageable.class))).thenReturn(List.of(userMessage));
        when(promptBuilder.buildChatPrompt("오늘 중요한 알림 알려줘", List.of(document), List.of(userMessage), Optional.of(timeRange)))
                .thenReturn(prompt);
        when(llmProvider.chat(prompt)).thenReturn("GitHub PR 리뷰 요청이 중요합니다.");

        final ChatMessageResponse response = chatService.chat(new ChatRequest("오늘 중요한 알림 알려줘"));

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.role()).isEqualTo("ASSISTANT");
        assertThat(response.content()).isEqualTo("GitHub PR 리뷰 요청이 중요합니다.");
        verify(timeRangeExtractor).extract("오늘 중요한 알림 알려줘");
        verify(ragRetriever).retrieve(1L, "오늘 중요한 알림 알려줘", Optional.of(timeRange));
        verify(promptBuilder).buildChatPrompt("오늘 중요한 알림 알려줘", List.of(document), List.of(userMessage), Optional.of(timeRange));
        verify(llmProvider).chat(prompt);
        verify(chatMessageRepository, org.mockito.Mockito.times(2)).save(savedMessageCaptor.capture());
        assertThat(savedMessageCaptor.getAllValues())
                .extracting(ChatMessage::getRole)
                .containsExactly(ChatMessageRole.USER, ChatMessageRole.ASSISTANT);
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamChatExtractsTimeRangeAndStreamsLlmChunksThenStoresAssistantMessage() {
        final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        final RagRetriever ragRetriever = mock(RagRetriever.class);
        final ChatTimeRangeExtractor timeRangeExtractor = mock(ChatTimeRangeExtractor.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final ChatService chatService = new ChatService(
                chatMessageRepository,
                ragRetriever,
                timeRangeExtractor,
                promptBuilder,
                llmProvider,
                aiProperties(),
                objectMapper(),
                chatMetrics()
        );
        final ChatMessage userMessage = message(
                1L,
                ChatMessageRole.USER,
                "최근 5시간 내의 알림 내역을 요약해줘",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        final ChatMessage assistantMessage = message(
                2L,
                ChatMessageRole.ASSISTANT,
                "GitHub PR 리뷰 요청",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 1, 0, ZoneOffset.UTC)
        );
        final TimeRange timeRange = new TimeRange(
                Instant.parse("2026-04-22T05:00:00Z"),
                Instant.parse("2026-04-22T10:00:00Z")
        );
        final LlmPrompt prompt = new LlmPrompt("system", "user");
        final ArgumentCaptor<ChatMessage> savedMessageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(userMessage)
                .thenReturn(assistantMessage);
        when(timeRangeExtractor.extract("최근 5시간 내의 알림 내역을 요약해줘")).thenReturn(Optional.of(timeRange));
        when(ragRetriever.retrieve(1L, "최근 5시간 내의 알림 내역을 요약해줘", Optional.of(timeRange)))
                .thenReturn(List.of());
        when(chatMessageRepository.findRecentByUserId(eq(1L), any(Pageable.class))).thenReturn(List.of(userMessage));
        when(promptBuilder.buildChatPrompt(
                "최근 5시간 내의 알림 내역을 요약해줘",
                List.of(),
                List.of(userMessage),
                Optional.of(timeRange)
        ))
                .thenReturn(prompt);
        org.mockito.Mockito.doAnswer(invocation -> {
            final Consumer<String> chunkConsumer = invocation.getArgument(1);
            chunkConsumer.accept("GitHub PR ");
            chunkConsumer.accept("리뷰 요청");
            return null;
        }).when(llmProvider).stream(eq(prompt), any(Consumer.class));

        chatService.streamChat(new ChatRequest("최근 5시간 내의 알림 내역을 요약해줘"));

        verify(llmProvider, org.mockito.Mockito.timeout(1000)).stream(eq(prompt), any(Consumer.class));
        verify(timeRangeExtractor).extract("최근 5시간 내의 알림 내역을 요약해줘");
        verify(ragRetriever).retrieve(1L, "최근 5시간 내의 알림 내역을 요약해줘", Optional.of(timeRange));
        verify(promptBuilder).buildChatPrompt(
                "최근 5시간 내의 알림 내역을 요약해줘",
                List.of(),
                List.of(userMessage),
                Optional.of(timeRange)
        );
        verify(chatMessageRepository, org.mockito.Mockito.timeout(2_000).atLeast(2)).save(savedMessageCaptor.capture());
        assertThat(savedMessageCaptor.getAllValues())
                .extracting(ChatMessage::getRole)
                .containsExactly(ChatMessageRole.USER, ChatMessageRole.ASSISTANT);
        assertThat(savedMessageCaptor.getAllValues().get(1).getContent()).isEqualTo("GitHub PR 리뷰 요청");
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamChatLogsSameCorrelationIdAndStreamIdAcrossStartedFirstChunkAndCompletedEvents() {
        final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        final RagRetriever ragRetriever = mock(RagRetriever.class);
        final ChatTimeRangeExtractor timeRangeExtractor = mock(ChatTimeRangeExtractor.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final ChatService chatService = new ChatService(
                chatMessageRepository,
                ragRetriever,
                timeRangeExtractor,
                promptBuilder,
                llmProvider,
                aiProperties(),
                objectMapper(),
                chatMetrics()
        );
        final ChatMessage userMessage = message(
                1L,
                ChatMessageRole.USER,
                "최근 5시간 내의 알림 내역을 요약해줘",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        final ChatMessage assistantMessage = message(
                2L,
                ChatMessageRole.ASSISTANT,
                "GitHub PR 리뷰 요청",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 1, 0, ZoneOffset.UTC)
        );
        final TimeRange timeRange = new TimeRange(
                Instant.parse("2026-04-22T05:00:00Z"),
                Instant.parse("2026-04-22T10:00:00Z")
        );
        final LlmPrompt prompt = new LlmPrompt("system", "user");
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(userMessage)
                .thenReturn(assistantMessage);
        when(timeRangeExtractor.extract("최근 5시간 내의 알림 내역을 요약해줘")).thenReturn(Optional.of(timeRange));
        when(ragRetriever.retrieve(1L, "최근 5시간 내의 알림 내역을 요약해줘", Optional.of(timeRange)))
                .thenReturn(List.of());
        when(chatMessageRepository.findRecentByUserId(eq(1L), any(Pageable.class))).thenReturn(List.of(userMessage));
        when(promptBuilder.buildChatPrompt(
                "최근 5시간 내의 알림 내역을 요약해줘",
                List.of(),
                List.of(userMessage),
                Optional.of(timeRange)
        )).thenReturn(prompt);
        org.mockito.Mockito.doAnswer(invocation -> {
            final Consumer<String> chunkConsumer = invocation.getArgument(1);
            chunkConsumer.accept("GitHub PR ");
            chunkConsumer.accept("리뷰 요청");
            return null;
        }).when(llmProvider).stream(eq(prompt), any(Consumer.class));

        final Logger logger = (Logger) LoggerFactory.getLogger(ChatService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        MDC.put("correlation_id", "corr-stream-success");

        try {
            chatService.streamChat(new ChatRequest("최근 5시간 내의 알림 내역을 요약해줘"));
            verify(chatMessageRepository, org.mockito.Mockito.timeout(1000).times(2)).save(any(ChatMessage.class));
            awaitLogCount(appender, 4);
        } finally {
            MDC.clear();
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        final List<ILoggingEvent> events = new ArrayList<>(appender.list);
        assertThat(events).hasSize(4);
        assertThat(events)
                .extracting(event -> event.getMDCPropertyMap().get("correlation_id"))
                .containsOnly("corr-stream-success");
        assertThat(events)
                .extracting(event -> event.getMDCPropertyMap().get("event"))
                .containsExactly(
                        "chat_stream_started",
                        "chat_prompt_built",
                        "chat_stream_first_chunk",
                        "chat_stream_completed"
                );

        final String streamId = extractStreamId(events.getFirst().getFormattedMessage());
        assertThat(streamId).isNotBlank();
        assertThat(events)
                .extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(message -> assertThat(message).contains("stream_id=" + streamId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamChatLogsSameCorrelationIdAndStreamIdAcrossFailureEvents() {
        final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        final RagRetriever ragRetriever = mock(RagRetriever.class);
        final ChatTimeRangeExtractor timeRangeExtractor = mock(ChatTimeRangeExtractor.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final ChatService chatService = new ChatService(
                chatMessageRepository,
                ragRetriever,
                timeRangeExtractor,
                promptBuilder,
                llmProvider,
                aiProperties(),
                objectMapper(),
                chatMetrics()
        );
        final ChatMessage userMessage = message(
                1L,
                ChatMessageRole.USER,
                "최근 5시간 내의 알림 내역을 요약해줘",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        final TimeRange timeRange = new TimeRange(
                Instant.parse("2026-04-22T05:00:00Z"),
                Instant.parse("2026-04-22T10:00:00Z")
        );
        final LlmPrompt prompt = new LlmPrompt("system", "user");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(userMessage);
        when(timeRangeExtractor.extract("최근 5시간 내의 알림 내역을 요약해줘")).thenReturn(Optional.of(timeRange));
        when(ragRetriever.retrieve(1L, "최근 5시간 내의 알림 내역을 요약해줘", Optional.of(timeRange)))
                .thenReturn(List.of());
        when(chatMessageRepository.findRecentByUserId(eq(1L), any(Pageable.class))).thenReturn(List.of(userMessage));
        when(promptBuilder.buildChatPrompt(
                "최근 5시간 내의 알림 내역을 요약해줘",
                List.of(),
                List.of(userMessage),
                Optional.of(timeRange)
        )).thenReturn(prompt);
        org.mockito.Mockito.doAnswer(invocation -> {
            final Consumer<String> chunkConsumer = invocation.getArgument(1);
            chunkConsumer.accept("GitHub PR ");
            throw new IllegalStateException("llm stream failed");
        }).when(llmProvider).stream(eq(prompt), any(Consumer.class));

        final Logger logger = (Logger) LoggerFactory.getLogger(ChatService.class);
        final Level previousLevel = logger.getLevel();
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        MDC.put("correlation_id", "corr-stream-failure");

        try {
            chatService.streamChat(new ChatRequest("최근 5시간 내의 알림 내역을 요약해줘"));
            verify(llmProvider, org.mockito.Mockito.timeout(1000)).stream(eq(prompt), any(Consumer.class));
            awaitLogCount(appender, 4);
        } finally {
            MDC.clear();
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        final List<ILoggingEvent> events = new ArrayList<>(appender.list);
        assertThat(events).hasSize(4);
        assertThat(events)
                .extracting(event -> event.getMDCPropertyMap().get("correlation_id"))
                .containsOnly("corr-stream-failure");
        assertThat(events)
                .extracting(event -> event.getMDCPropertyMap().get("event"))
                .containsExactly(
                        "chat_stream_started",
                        "chat_prompt_built",
                        "chat_stream_first_chunk",
                        "chat_stream_failed"
                );

        final String streamId = extractStreamId(events.getFirst().getFormattedMessage());
        assertThat(events)
                .extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(message -> assertThat(message).contains("stream_id=" + streamId));
        assertThat(events.getLast().getLevel()).isEqualTo(Level.WARN);
        assertThat(events.getLast().getThrowableProxy()).isNotNull();
    }

    @Test
    void chatUsesEmptyTimeRangeWhenQuestionHasNoTimeExpression() {
        final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        final RagRetriever ragRetriever = mock(RagRetriever.class);
        final ChatTimeRangeExtractor timeRangeExtractor = mock(ChatTimeRangeExtractor.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final ChatService chatService = new ChatService(
                chatMessageRepository,
                ragRetriever,
                timeRangeExtractor,
                promptBuilder,
                llmProvider,
                aiProperties(),
                objectMapper(),
                chatMetrics()
        );
        final ChatMessage userMessage = message(
                1L,
                ChatMessageRole.USER,
                "중요한 알림 알려줘",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        final ChatMessage assistantMessage = message(
                2L,
                ChatMessageRole.ASSISTANT,
                "중요한 알림이 없습니다.",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 1, 0, ZoneOffset.UTC)
        );
        final LlmPrompt prompt = new LlmPrompt("system", "user");

        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(userMessage)
                .thenReturn(assistantMessage);
        when(timeRangeExtractor.extract("중요한 알림 알려줘")).thenReturn(Optional.empty());
        when(ragRetriever.retrieve(1L, "중요한 알림 알려줘", Optional.empty())).thenReturn(List.of());
        when(chatMessageRepository.findRecentByUserId(eq(1L), any(Pageable.class))).thenReturn(List.of(userMessage));
        when(promptBuilder.buildChatPrompt("중요한 알림 알려줘", List.of(), List.of(userMessage), Optional.empty()))
                .thenReturn(prompt);
        when(llmProvider.chat(prompt)).thenReturn("중요한 알림이 없습니다.");

        final ChatMessageResponse response = chatService.chat(new ChatRequest("중요한 알림 알려줘"));

        assertThat(response.content()).isEqualTo("중요한 알림이 없습니다.");
        verify(timeRangeExtractor).extract("중요한 알림 알려줘");
        verify(ragRetriever).retrieve(1L, "중요한 알림 알려줘", Optional.empty());
        verify(promptBuilder).buildChatPrompt("중요한 알림 알려줘", List.of(), List.of(userMessage), Optional.empty());
    }

    @Test
    void historyReadsRecentMessagesFromRepository() {
        final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        final RagRetriever ragRetriever = mock(RagRetriever.class);
        final ChatTimeRangeExtractor timeRangeExtractor = mock(ChatTimeRangeExtractor.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final ChatService chatService = new ChatService(
                chatMessageRepository,
                ragRetriever,
                timeRangeExtractor,
                promptBuilder,
                llmProvider,
                aiProperties(),
                objectMapper(),
                chatMetrics()
        );
        final ChatMessage message = message(
                10L,
                ChatMessageRole.ASSISTANT,
                "최근 알림 요약입니다.",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(chatMessageRepository.findRecentByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(message));

        final List<ChatMessageResponse> history = chatService.history();

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().id()).isEqualTo(10L);
        assertThat(history.getFirst().role()).isEqualTo("ASSISTANT");
        assertThat(history.getFirst().content()).isEqualTo("최근 알림 요약입니다.");
        verify(chatMessageRepository).findRecentByUserId(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    private ChatMessage message(
            final Long id,
            final ChatMessageRole role,
            final String content,
            final OffsetDateTime createdAt
    ) {
        final ChatMessage message = new ChatMessage(1L, role, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        ReflectionTestUtils.setField(message, "updatedAt", createdAt);
        return message;
    }

    private NotioAiProperties aiProperties() {
        return new NotioAiProperties(
                Duration.ofSeconds(30),
                Duration.ofSeconds(15),
                Duration.ofSeconds(5)
        );
    }

    private ObjectMapper objectMapper() {
        return new JacksonConfig().objectMapper();
    }

    private ChatMetrics chatMetrics() {
        final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        return new ChatMetrics(new NotioMetrics(meterRegistry, new NotioMetricsTagPolicy()));
    }

    private void awaitLogCount(final ListAppender<ILoggingEvent> appender, final int expectedCount) {
        final long deadline = System.currentTimeMillis() + 1_000L;
        while (System.currentTimeMillis() < deadline) {
            if (appender.list.size() >= expectedCount) {
                return;
            }
            try {
                Thread.sleep(10L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for chat logs", exception);
            }
        }
        throw new AssertionError("Expected at least " + expectedCount + " log events but found " + appender.list.size());
    }

    private String extractStreamId(final String message) {
        final String marker = "stream_id=";
        final int start = message.indexOf(marker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        final int end = message.indexOf(' ', start);
        if (end < 0) {
            return message.substring(start + marker.length());
        }
        return message.substring(start + marker.length(), end);
    }
}
