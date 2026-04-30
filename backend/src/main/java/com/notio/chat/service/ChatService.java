package com.notio.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.ai.llm.LlmProvider;
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
import com.notio.common.config.properties.NotioAiProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class ChatService {

    private static final Long DEFAULT_PHASE0_USER_ID = 1L;
    private static final int HISTORY_LIMIT = 50;

    private final ChatMessageRepository chatMessageRepository;
    private final RagRetriever ragRetriever;
    private final ChatTimeRangeExtractor timeRangeExtractor;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final NotioAiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final ChatMetrics chatMetrics;

    public ChatService(
            final ChatMessageRepository chatMessageRepository,
            final RagRetriever ragRetriever,
            final ChatTimeRangeExtractor timeRangeExtractor,
            final PromptBuilder promptBuilder,
            final LlmProvider llmProvider,
            final NotioAiProperties aiProperties,
            final ObjectMapper objectMapper,
            final ChatMetrics chatMetrics
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.ragRetriever = ragRetriever;
        this.timeRangeExtractor = timeRangeExtractor;
        this.promptBuilder = promptBuilder;
        this.llmProvider = llmProvider;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.chatMetrics = chatMetrics;
    }

    public ChatMessageResponse chat(final ChatRequest request) {
        final Instant startedAt = Instant.now();
        final ChatMessage userMessage = appendMessage(ChatMessageRole.USER, request.content());
        final ChatPromptContext promptContext = buildChatPromptContext(request.content(), userMessage.getUserId());

        logChatRequestStarted(promptContext);
        logChatPromptBuilt(promptContext, "sync");

        try {
            final String responseText = llmProvider.chat(promptContext.prompt());
            final ChatMessageResponse response = append(ChatMessageRole.ASSISTANT, responseText);
            chatMetrics.recordResponseChars("sync", responseText.length());
            chatMetrics.recordChatRequest("sync", "success", Duration.between(startedAt, Instant.now()));
            return response;
        } catch (RuntimeException exception) {
            chatMetrics.recordChatRequest("sync", "failure", Duration.between(startedAt, Instant.now()));
            throw exception;
        }
    }

    public SseEmitter streamChat(final ChatRequest request) {
        final String streamId = UUID.randomUUID().toString();
        final Instant startedAt = Instant.now();
        final ChatMessage userMessage = appendMessage(ChatMessageRole.USER, request.content());
        final ChatPromptContext promptContext = buildChatPromptContext(request.content(), userMessage.getUserId());

        logChatStreamStarted(streamId, promptContext);
        logChatPromptBuilt(promptContext, streamId);

        final SseEmitter emitter = new SseEmitter(aiProperties.streamingTimeout().toMillis());
        final AtomicBoolean active = new AtomicBoolean(true);
        final AtomicBoolean finalized = new AtomicBoolean(false);
        final AtomicInteger chunkCount = new AtomicInteger();
        final AtomicLong responseCharacters = new AtomicLong();
        final AtomicLong firstChunkElapsedMs = new AtomicLong(-1);
        final Map<String, String> loggingContext = captureLoggingContext(streamId);

        chatMetrics.incrementActiveStreams();

        emitter.onCompletion(() -> {
            active.set(false);
            finalizeStream(
                    finalized,
                    loggingContext,
                    "chat_stream_completed",
                    "success",
                    streamId,
                    chunkCount,
                    responseCharacters,
                    firstChunkElapsedMs,
                    startedAt,
                    null
            );
        });
        emitter.onTimeout(() -> {
            active.set(false);
            finalizeStream(
                    finalized,
                    loggingContext,
                    "chat_stream_timed_out",
                    "timeout",
                    streamId,
                    chunkCount,
                    responseCharacters,
                    firstChunkElapsedMs,
                    startedAt,
                    null
            );
        });
        emitter.onError(exception -> {
            active.set(false);
            finalizeStream(
                    finalized,
                    loggingContext,
                    "chat_stream_failed",
                    "failure",
                    streamId,
                    chunkCount,
                    responseCharacters,
                    firstChunkElapsedMs,
                    startedAt,
                    exception
            );
        });

        Thread.startVirtualThread(() -> {
            final StringBuilder assistantContent = new StringBuilder();
            applyLoggingContext(loggingContext);
            try {
                llmProvider.stream(promptContext.prompt(), chunk -> {
                    if (!active.get()) {
                        throw new CancellationException("SSE client disconnected");
                    }
                    if (chunkCount.get() == 0) {
                        firstChunkElapsedMs.set(elapsedMillis(startedAt));
                        chatMetrics.recordFirstChunk(Duration.ofMillis(firstChunkElapsedMs.get()));
                        logStreamFirstChunk(streamId, promptContext, firstChunkElapsedMs.get(), chunk.length());
                    }
                    chunkCount.incrementAndGet();
                    responseCharacters.addAndGet(chunk.length());
                    assistantContent.append(chunk);
                    sendChunk(emitter, chunk);
                });

                if (!active.get()) {
                    finalizeStream(
                            finalized,
                            loggingContext,
                            "chat_stream_cancelled",
                            "cancelled",
                            streamId,
                            chunkCount,
                            responseCharacters,
                            firstChunkElapsedMs,
                            startedAt,
                            null
                    );
                    return;
                }

                final ChatMessage assistantMessage = appendMessage(
                        ChatMessageRole.ASSISTANT,
                        assistantContent.toString().trim()
                );
                chatMetrics.recordResponseChars("stream", responseCharacters.get());
                emitter.send(SseEmitter.event()
                        .data(toJson(Map.of("done", true, "message_id", assistantMessage.getId()))));
                finalizeStream(
                        finalized,
                        loggingContext,
                        "chat_stream_completed",
                        "success",
                        streamId,
                        chunkCount,
                        responseCharacters,
                        firstChunkElapsedMs,
                        startedAt,
                        null
                );
                emitter.complete();
            } catch (CancellationException exception) {
                active.set(false);
                finalizeStream(
                        finalized,
                        loggingContext,
                        "chat_stream_cancelled",
                        "cancelled",
                        streamId,
                        chunkCount,
                        responseCharacters,
                        firstChunkElapsedMs,
                        startedAt,
                        null
                );
            } catch (Exception exception) {
                active.set(false);
                finalizeStream(
                        finalized,
                        loggingContext,
                        "chat_stream_failed",
                        "failure",
                        streamId,
                        chunkCount,
                        responseCharacters,
                        firstChunkElapsedMs,
                        startedAt,
                        exception
                );
                emitter.completeWithError(exception);
            } finally {
                MDC.clear();
            }
        });

        return emitter;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> history() {
        return chatMessageRepository.findRecentByUserId(
                        DEFAULT_PHASE0_USER_ID,
                        PageRequest.of(0, HISTORY_LIMIT)
                ).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    private ChatMessageResponse append(final ChatMessageRole role, final String content) {
        return ChatMessageResponse.from(appendMessage(role, content));
    }

    private ChatMessage appendMessage(final ChatMessageRole role, final String content) {
        final ChatMessage message = new ChatMessage(DEFAULT_PHASE0_USER_ID, role, content);
        return chatMessageRepository.save(message);
    }

    private ChatPromptContext buildChatPromptContext(final String userMessage, final Long userId) {
        final Optional<TimeRange> timeRange = timeRangeExtractor.extract(userMessage);
        final List<RagDocument> documents = ragRetriever.retrieve(userId, userMessage, timeRange);
        final List<ChatMessage> recentMessages = chatMessageRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, 10)
        );
        final var prompt = promptBuilder.buildChatPrompt(userMessage, documents, recentMessages, timeRange);
        return new ChatPromptContext(
                prompt,
                timeRange.isPresent(),
                recentMessages.size(),
                documents.size(),
                prompt.system().length() + prompt.user().length()
        );
    }

    private void logChatRequestStarted(final ChatPromptContext promptContext) {
        putEventContext("chat_request_started", "started");
        try {
            log.info(
                    "event=chat_request_started stream_id=sync time_range_applied={} history_count={}",
                    promptContext.timeRangeApplied(),
                    promptContext.historyCount()
            );
        } finally {
            clearEventContext();
        }
    }

    private void logChatStreamStarted(final String streamId, final ChatPromptContext promptContext) {
        putEventContext("chat_stream_started", "started");
        try {
            log.info(
                    "event=chat_stream_started stream_id={} time_range_applied={} history_count={}",
                    streamId,
                    promptContext.timeRangeApplied(),
                    promptContext.historyCount()
            );
        } finally {
            clearEventContext();
        }
    }

    private void logChatPromptBuilt(final ChatPromptContext promptContext, final String streamId) {
        putEventContext("chat_prompt_built", "success");
        try {
            log.info(
                    "event=chat_prompt_built stream_id={} time_range_applied={} history_count={} rag_result_count={} prompt_chars={}",
                    streamId,
                    promptContext.timeRangeApplied(),
                    promptContext.historyCount(),
                    promptContext.ragResultCount(),
                    promptContext.promptChars()
            );
        } finally {
            clearEventContext();
        }
    }

    private void logStreamFirstChunk(
            final String streamId,
            final ChatPromptContext promptContext,
            final long firstChunkElapsedMs,
            final int chunkChars
    ) {
        putEventContext("chat_stream_first_chunk", "success");
        try {
            log.info(
                    "event=chat_stream_first_chunk stream_id={} time_range_applied={} history_count={} first_chunk_elapsed_ms={} chunk_chars={}",
                    streamId,
                    promptContext.timeRangeApplied(),
                    promptContext.historyCount(),
                    firstChunkElapsedMs,
                    chunkChars
            );
        } finally {
            clearEventContext();
        }
    }

    private void finalizeStream(
            final AtomicBoolean finalized,
            final Map<String, String> loggingContext,
            final String event,
            final String outcome,
            final String streamId,
            final AtomicInteger chunkCount,
            final AtomicLong responseCharacters,
            final AtomicLong firstChunkElapsedMs,
            final Instant startedAt,
            final Throwable throwable
    ) {
        if (!finalized.compareAndSet(false, true)) {
            return;
        }

        applyLoggingContext(loggingContext);
        putEventContext(event, outcome);
        try {
            final String message =
                    "event=%s stream_id=%s chunk_count=%d response_chars=%d first_chunk_elapsed_ms=%d elapsed_ms=%d"
                            .formatted(
                                    event,
                                    streamId,
                                    chunkCount.get(),
                                    responseCharacters.get(),
                                    firstChunkElapsedMs.get(),
                                    elapsedMillis(startedAt)
                            );
            if (throwable == null) {
                if ("failure".equals(outcome) || "timeout".equals(outcome)) {
                    log.warn(message);
                } else {
                    log.info(message);
                }
            } else {
                log.warn(message, throwable);
            }
        } finally {
            clearEventContext();
            chatMetrics.recordChatRequest("stream", outcome, Duration.between(startedAt, Instant.now()));
            chatMetrics.decrementActiveStreams();
            MDC.clear();
        }
    }

    private Map<String, String> captureLoggingContext(final String streamId) {
        final Map<String, String> context = MDC.getCopyOfContextMap();
        final Map<String, String> copy = context == null ? new HashMap<>() : new HashMap<>(context);
        copy.put("stream_id", streamId);
        return copy;
    }

    private void applyLoggingContext(final Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            MDC.clear();
            return;
        }
        MDC.setContextMap(context);
    }

    private void putEventContext(final String event, final String outcome) {
        MDC.put("event", event);
        MDC.put("outcome", outcome);
    }

    private void clearEventContext() {
        MDC.remove("outcome");
        MDC.remove("event");
    }

    private long elapsedMillis(final Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }

    private void sendChunk(final SseEmitter emitter, final String chunk) {
        try {
            emitter.send(SseEmitter.event().data(toJson(Map.of("chunk", chunk))));
        } catch (Exception exception) {
            throw new CancellationException("SSE client disconnected");
        }
    }

    private String toJson(final Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize SSE payload", exception);
        }
    }
}
