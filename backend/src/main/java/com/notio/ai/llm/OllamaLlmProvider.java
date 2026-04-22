package com.notio.ai.llm;

import com.notio.ai.prompt.LlmPrompt;
import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.exception.AiExceptionTranslator;
import java.util.concurrent.CancellationException;
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
            final ChatResponse response = chatModel.call(toSpringPrompt(prompt));
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
