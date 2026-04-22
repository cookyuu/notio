package com.notio.ai.llm;

import com.notio.ai.prompt.LlmPrompt;

public interface LlmProvider {

    String chat(LlmPrompt prompt);
}
