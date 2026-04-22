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
import java.util.ArrayList;
import java.util.List;
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

    public ChatService(
            final ChatMessageRepository chatMessageRepository,
            final RagRetriever ragRetriever,
            final PromptBuilder promptBuilder,
            final LlmProvider llmProvider
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.ragRetriever = ragRetriever;
        this.promptBuilder = promptBuilder;
        this.llmProvider = llmProvider;
    }

    public ChatMessageResponse chat(final ChatRequest request) {
        final ChatMessage userMessage = appendMessage(ChatMessageRole.USER, request.content());
        final String responseText = generateAiResponse(request.content(), userMessage.getUserId());
        return append(ChatMessageRole.ASSISTANT, responseText);
    }

    public SseEmitter streamChat(final ChatRequest request) {
        final ChatMessage userMessage = appendMessage(ChatMessageRole.USER, request.content());
        final String responseText = generateAiResponse(request.content(), userMessage.getUserId());
        final SseEmitter emitter = new SseEmitter(30_000L);

        new Thread(() -> {
            try {
                final String[] chunks = splitIntoChunks(responseText);
                for (final String chunk : chunks) {
                    emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    Thread.sleep(50);
                }
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                append(ChatMessageRole.ASSISTANT, responseText);
                emitter.complete();
            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        }).start();

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

    private String generateAiResponse(final String userMessage, final Long userId) {
        final List<RagDocument> documents = ragRetriever.retrieve(userId, userMessage);
        final List<ChatMessage> recentMessages = chatMessageRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, 10)
        );
        final LlmPrompt prompt = promptBuilder.buildChatPrompt(userMessage, documents, recentMessages);
        return llmProvider.chat(prompt);
    }

    private String[] splitIntoChunks(final String text) {
        final List<String> chunks = new ArrayList<>();
        final String[] words = text.split("\\s+");

        for (int i = 0; i < words.length; i += 3) {
            final StringBuilder chunk = new StringBuilder();
            for (int j = i; j < Math.min(i + 3, words.length); j++) {
                chunk.append(words[j]).append(" ");
            }
            chunks.add(chunk.toString().trim());
        }

        return chunks.toArray(new String[0]);
    }
}

