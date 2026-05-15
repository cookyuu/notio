package com.notio.ai.prompt;

public record LlmPrompt(
        String system,
        String user
) {

    public static LlmPrompt of(String system, String user) {
        return new LlmPrompt(system, user);
    }
}
