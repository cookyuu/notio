package com.notio.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.ai.rag.RagDocument;
import com.notio.ai.rag.RagRetriever;
import com.notio.chat.domain.ChatMessage;
import com.notio.chat.domain.ChatMessageRole;
import com.notio.chat.dto.ChatMessageResponse;
import com.notio.chat.dto.ChatRequest;
import com.notio.chat.repository.ChatMessageRepository;
import com.notio.common.config.properties.NotioAiProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
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
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final NotioAiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public ChatService(
            final ChatMessageRepository chatMessageRepository,
            final RagRetriever ragRetriever,
            final PromptBuilder promptBuilder,
            final LlmProvider llmProvider,
            final NotioAiProperties aiProperties,
            final ObjectMapper objectMapper
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.ragRetriever = ragRetriever;
        this.promptBuilder = promptBuilder;
        this.llmProvider = llmProvider;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    public ChatMessageResponse chat(final ChatRequest request) {
        final ChatMessage userMessage = appendMessage(ChatMessageRole.USER, request.content());
        final LlmPrompt prompt = buildChatPrompt(request.content(), userMessage.getUserId());
        final String responseText = llmProvider.chat(prompt);
        return append(ChatMessageRole.ASSISTANT, responseText);
    }

    public SseEmitter streamChat(final ChatRequest request) {
        final String streamId = UUID.randomUUID().toString();
        final Instant startedAt = Instant.now();
        log.info(
                "Chat stream request received: streamId={}, contentLength={}",
                streamId,
                request.content().length()
        );

        final ChatMessage userMessage = appendMessage(ChatMessageRole.USER, request.content());
        log.info(
                "Chat stream user message saved: streamId={}, userId={}, messageId={}, elapsedMs={}",
                streamId,
                userMessage.getUserId(),
                userMessage.getId(),
                elapsedMillis(startedAt)
        );

        final LlmPrompt prompt = buildChatPrompt(request.content(), userMessage.getUserId(), streamId, startedAt);
        log.info(
                "Chat stream prompt ready: streamId={}, elapsedMs={}",
                streamId,
                elapsedMillis(startedAt)
        );

        final SseEmitter emitter = new SseEmitter(aiProperties.streamingTimeout().toMillis());
        final AtomicBoolean active = new AtomicBoolean(true);
        final AtomicInteger chunkCount = new AtomicInteger();
        final AtomicLong responseCharacters = new AtomicLong();
        final AtomicLong firstChunkElapsedMs = new AtomicLong(-1);

        emitter.onCompletion(() -> {
            active.set(false);
            log.info(
                    "Chat stream emitter completed: streamId={}, chunks={}, responseChars={}, firstChunkElapsedMs={}, elapsedMs={}",
                    streamId,
                    chunkCount.get(),
                    responseCharacters.get(),
                    firstChunkElapsedMs.get(),
                    elapsedMillis(startedAt)
            );
        });
        emitter.onTimeout(() -> {
            active.set(false);
            log.warn(
                    "Chat stream emitter timed out: streamId={}, timeoutMs={}, chunks={}, responseChars={}, firstChunkElapsedMs={}, elapsedMs={}",
                    streamId,
                    aiProperties.streamingTimeout().toMillis(),
                    chunkCount.get(),
                    responseCharacters.get(),
                    firstChunkElapsedMs.get(),
                    elapsedMillis(startedAt)
            );
        });
        emitter.onError(exception -> {
            active.set(false);
            log.warn(
                    "Chat stream emitter error: streamId={}, chunks={}, responseChars={}, firstChunkElapsedMs={}, elapsedMs={}",
                    streamId,
                    chunkCount.get(),
                    responseCharacters.get(),
                    firstChunkElapsedMs.get(),
                    elapsedMillis(startedAt),
                    exception
            );
        });

        Thread.startVirtualThread(() -> {
            final StringBuilder assistantContent = new StringBuilder();
            try {
                log.info("Chat stream LLM call started: streamId={}, elapsedMs={}", streamId, elapsedMillis(startedAt));
                llmProvider.stream(prompt, chunk -> {
                    if (!active.get()) {
                        throw new CancellationException("SSE client disconnected");
                    }
                    if (chunkCount.get() == 0) {
                        firstChunkElapsedMs.set(elapsedMillis(startedAt));
                        log.info(
                                "Chat stream first chunk received: streamId={}, firstChunkElapsedMs={}, chunkChars={}",
                                streamId,
                                firstChunkElapsedMs.get(),
                                chunk.length()
                        );
                    }
                    chunkCount.incrementAndGet();
                    responseCharacters.addAndGet(chunk.length());
                    assistantContent.append(chunk);
                    sendChunk(emitter, chunk);
                });

                if (!active.get()) {
                    log.info(
                            "Chat stream stopped after client disconnect: streamId={}, chunks={}, responseChars={}, elapsedMs={}",
                            streamId,
                            chunkCount.get(),
                            responseCharacters.get(),
                            elapsedMillis(startedAt)
                    );
                    return;
                }

                final ChatMessage assistantMessage = appendMessage(
                        ChatMessageRole.ASSISTANT,
                        assistantContent.toString().trim()
                );
                emitter.send(SseEmitter.event()
                        .data(toJson(Map.of("done", true, "message_id", assistantMessage.getId()))));
                log.info(
                        "Chat stream done sent: streamId={}, assistantMessageId={}, chunks={}, responseChars={}, firstChunkElapsedMs={}, elapsedMs={}",
                        streamId,
                        assistantMessage.getId(),
                        chunkCount.get(),
                        responseCharacters.get(),
                        firstChunkElapsedMs.get(),
                        elapsedMillis(startedAt)
                );
                emitter.complete();
            } catch (CancellationException exception) {
                active.set(false);
                log.info(
                        "Chat stream cancelled: streamId={}, chunks={}, responseChars={}, elapsedMs={}",
                        streamId,
                        chunkCount.get(),
                        responseCharacters.get(),
                        elapsedMillis(startedAt)
                );
            } catch (Exception exception) {
                log.warn(
                        "Chat stream failed: streamId={}, chunks={}, responseChars={}, firstChunkElapsedMs={}, elapsedMs={}",
                        streamId,
                        chunkCount.get(),
                        responseCharacters.get(),
                        firstChunkElapsedMs.get(),
                        elapsedMillis(startedAt),
                        exception
                );
                if (active.get()) {
                    emitter.completeWithError(exception);
                }
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

    private LlmPrompt buildChatPrompt(final String userMessage, final Long userId) {
        return buildChatPrompt(userMessage, userId, null, null);
    }

    private LlmPrompt buildChatPrompt(
            final String userMessage,
            final Long userId,
            final String streamId,
            final Instant startedAt
    ) {
        logTimed(streamId, startedAt, "Chat stream RAG retrieval started");
        final List<RagDocument> documents = ragRetriever.retrieve(userId, userMessage);
        logTimed(streamId, startedAt, "Chat stream RAG retrieval completed: documents=%d".formatted(documents.size()));
        logTimed(streamId, startedAt, "Chat stream recent history retrieval started");
        final List<ChatMessage> recentMessages = chatMessageRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, 10)
        );
        logTimed(
                streamId,
                startedAt,
                "Chat stream recent history retrieval completed: messages=%d".formatted(recentMessages.size())
        );
        return promptBuilder.buildChatPrompt(userMessage, documents, recentMessages);
    }

    private void logTimed(final String streamId, final Instant startedAt, final String message) {
        if (streamId == null || startedAt == null) {
            return;
        }
        log.info("{}: streamId={}, elapsedMs={}", message, streamId, elapsedMillis(startedAt));
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

