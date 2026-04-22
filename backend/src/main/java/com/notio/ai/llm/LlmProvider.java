package com.notio.ai.llm;

import com.notio.ai.prompt.LlmPrompt;
import java.util.function.Consumer;

public interface LlmProvider {

    String chat(LlmPrompt prompt);

    void stream(LlmPrompt prompt, Consumer<String> chunkConsumer);
}
