package com.notio.ai.llm;

import com.notio.ai.metrics.LlmMetrics;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.exception.AiExceptionTranslator;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "notio.ai.provider", havingValue = "anthropic")
@RequiredArgsConstructor
public class AnthropicLlmProvider implements LlmProvider {

    private final ChatModel anthropicChatModel;
    private final NotioAiProperties aiProperties;
    private final AiExceptionTranslator exceptionTranslator;
    private final LlmMetrics llmMetrics;

    @Override
    public String chat(LlmPrompt prompt) {
        Instant start = Instant.now();
        try {
            var response = anthropicChatModel.call(
                new Prompt(List.of(
                    new SystemMessage(prompt.system()),
                    new UserMessage(prompt.user())
                ))
            );
            String result = response.getResult().getOutput().getText();
            llmMetrics.recordLlmCall("success", Duration.between(start, Instant.now()));
            return result;
        } catch (Exception e) {
            llmMetrics.recordLlmCall("failure", Duration.between(start, Instant.now()));
            throw exceptionTranslator.llmUnavailable(e);
        }
    }

    @Override
    public void stream(LlmPrompt prompt, Consumer<String> chunkConsumer) {
        throw new UnsupportedOperationException("Streaming not used in summary pipeline");
    }
}
