package com.notio.chat.service;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatService {

    private static final Long DEFAULT_PHASE0_USER_ID = 1L;
    private static final int HISTORY_LIMIT = 50;

    private final ChatMessageRepository chatMessageRepository;
    private final RagRetriever ragRetriever;
    private final PromptBuilder promptBuilder;
    private final LlmProvider llmProvider;
    private final NotioAiProperties aiProperties;

    public ChatService(
            final ChatMessageRepository chatMessageRepository,
            final RagRetriever ragRetriever,
            final PromptBuilder promptBuilder,
            final LlmProvider llmProvider,
            final NotioAiProperties aiProperties
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.ragRetriever = ragRetriever;
        this.promptBuilder = promptBuilder;
        this.llmProvider = llmProvider;
        this.aiProperties = aiProperties;
    }

    public ChatMessageResponse chat(final ChatRequest request) {
        final ChatMessage userMessage = appendMessage(ChatMessageRole.USER, request.content());
        final LlmPrompt prompt = buildChatPrompt(request.content(), userMessage.getUserId());
        final String responseText = llmProvider.chat(prompt);
        return append(ChatMessageRole.ASSISTANT, responseText);
    }

    public SseEmitter streamChat(final ChatRequest request) {
        final ChatMessage userMessage = appendMessage(ChatMessageRole.USER, request.content());
        final LlmPrompt prompt = buildChatPrompt(request.content(), userMessage.getUserId());
        final SseEmitter emitter = new SseEmitter(aiProperties.streamingTimeout().toMillis());
        final AtomicBoolean active = new AtomicBoolean(true);

        emitter.onCompletion(() -> active.set(false));
        emitter.onTimeout(() -> active.set(false));
        emitter.onError(exception -> active.set(false));

        Thread.startVirtualThread(() -> {
            final StringBuilder assistantContent = new StringBuilder();
            try {
                llmProvider.stream(prompt, chunk -> {
                    if (!active.get()) {
                        throw new CancellationException("SSE client disconnected");
                    }
                    assistantContent.append(chunk);
                    sendChunk(emitter, chunk);
                });

                if (!active.get()) {
                    return;
                }

                final ChatMessage assistantMessage = appendMessage(
                        ChatMessageRole.ASSISTANT,
                        assistantContent.toString().trim()
                );
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of("done", true, "message_id", assistantMessage.getId())));
                emitter.complete();
            } catch (CancellationException exception) {
                active.set(false);
            } catch (Exception exception) {
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
        final List<RagDocument> documents = ragRetriever.retrieve(userId, userMessage);
        final List<ChatMessage> recentMessages = chatMessageRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, 10)
        );
        return promptBuilder.buildChatPrompt(userMessage, documents, recentMessages);
    }

    private void sendChunk(final SseEmitter emitter, final String chunk) {
        try {
            emitter.send(SseEmitter.event().name("chunk").data(chunk));
        } catch (Exception exception) {
            throw new CancellationException("SSE client disconnected");
        }
    }
}

