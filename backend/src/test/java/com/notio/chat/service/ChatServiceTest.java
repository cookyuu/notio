package com.notio.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.notio.chat.repository.ChatMessageRepository;
import com.notio.common.config.JacksonConfig;
import com.notio.common.config.properties.NotioAiProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
                objectMapper()
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
    void streamChatStreamsLlmChunksThenStoresAssistantMessage() {
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
                objectMapper()
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
                "GitHub PR 리뷰 요청",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 1, 0, ZoneOffset.UTC)
        );
        final LlmPrompt prompt = new LlmPrompt("system", "user");
        final ArgumentCaptor<ChatMessage> savedMessageCaptor = ArgumentCaptor.forClass(ChatMessage.class);

        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(userMessage)
                .thenReturn(assistantMessage);
        when(timeRangeExtractor.extract("오늘 중요한 알림 알려줘")).thenReturn(Optional.empty());
        when(ragRetriever.retrieve(1L, "오늘 중요한 알림 알려줘", Optional.empty())).thenReturn(List.of());
        when(chatMessageRepository.findRecentByUserId(eq(1L), any(Pageable.class))).thenReturn(List.of(userMessage));
        when(promptBuilder.buildChatPrompt("오늘 중요한 알림 알려줘", List.of(), List.of(userMessage), Optional.empty()))
                .thenReturn(prompt);
        org.mockito.Mockito.doAnswer(invocation -> {
            final Consumer<String> chunkConsumer = invocation.getArgument(1);
            chunkConsumer.accept("GitHub PR ");
            chunkConsumer.accept("리뷰 요청");
            return null;
        }).when(llmProvider).stream(eq(prompt), any(Consumer.class));

        chatService.streamChat(new ChatRequest("오늘 중요한 알림 알려줘"));

        verify(llmProvider, org.mockito.Mockito.timeout(1000)).stream(eq(prompt), any(Consumer.class));
        verify(timeRangeExtractor).extract("오늘 중요한 알림 알려줘");
        verify(chatMessageRepository, org.mockito.Mockito.timeout(1000).times(2)).save(savedMessageCaptor.capture());
        assertThat(savedMessageCaptor.getAllValues())
                .extracting(ChatMessage::getRole)
                .containsExactly(ChatMessageRole.USER, ChatMessageRole.ASSISTANT);
        assertThat(savedMessageCaptor.getAllValues().get(1).getContent()).isEqualTo("GitHub PR 리뷰 요청");
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
                objectMapper()
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
}
