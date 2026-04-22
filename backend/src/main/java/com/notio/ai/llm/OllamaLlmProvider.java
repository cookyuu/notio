package com.notio.ai.llm;

import com.notio.ai.prompt.LlmPrompt;
import com.notio.common.exception.AiExceptionTranslator;
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

    @Override
    public String chat(final LlmPrompt prompt) {
        try {
            final ChatResponse response = chatModel.call(new Prompt(
                    new SystemMessage(prompt.system()),
                    new UserMessage(prompt.user())
            ));
            return extractContent(response);
        } catch (RuntimeException exception) {
            throw exceptionTranslator.llmUnavailable(exception);
        }
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
}
