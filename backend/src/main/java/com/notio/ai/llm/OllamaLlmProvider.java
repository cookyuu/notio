package com.notio.ai.llm;

import com.notio.ai.prompt.LlmPrompt;
import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.exception.AiExceptionTranslator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OllamaLlmProvider implements LlmProvider {

    private final ChatModel chatModel;
    private final AiExceptionTranslator exceptionTranslator;
    private final NotioAiProperties aiProperties;

    @Override
    public String chat(final LlmPrompt prompt) {
        try {
            final ChatResponse response = callWithTimeout(prompt);
            return extractContent(response);
        } catch (RuntimeException exception) {
            throw exceptionTranslator.llmUnavailable(exception);
        }
    }

    @Override
    public void stream(final LlmPrompt prompt, final Consumer<String> chunkConsumer) {
        try {
            chatModel.stream(toSpringPrompt(prompt))
                    .timeout(aiProperties.streamingTimeout())
                    .map(this::extractChunk)
                    .filter(chunk -> !chunk.isBlank())
                    .doOnNext(chunkConsumer)
                    .blockLast(aiProperties.streamingTimeout());
        } catch (CancellationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw exceptionTranslator.llmUnavailable(exception);
        }
    }

    private Prompt toSpringPrompt(final LlmPrompt prompt) {
        return new Prompt(
                new SystemMessage(prompt.system()),
                new UserMessage(prompt.user())
        );
    }

    private ChatResponse callWithTimeout(final LlmPrompt prompt) {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            final Future<ChatResponse> future = executorService.submit(() -> chatModel.call(toSpringPrompt(prompt)));
            try {
                return future.get(aiProperties.llmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException exception) {
                future.cancel(true);
                throw new IllegalStateException("LLM request timed out", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                future.cancel(true);
                throw new IllegalStateException("LLM request interrupted", exception);
            } catch (ExecutionException exception) {
                throw unwrapExecutionException(exception);
            }
        }
    }

    private RuntimeException unwrapExecutionException(final ExecutionException exception) {
        final Throwable cause = exception.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException("LLM request failed", cause);
    }

    private String extractContent(final ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("LLM response is empty");
        }

        final String content = response.getResult().getOutput().getText();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("LLM response content is blank");
        }
        return content.trim();
    }

    private String extractChunk(final ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }

        final String content = response.getResult().getOutput().getText();
        if (content == null) {
            return "";
        }
        return content;
    }
}
