package com.notio.chat.service;

import com.notio.ai.prompt.LlmPrompt;

record ChatPromptContext(
        LlmPrompt prompt,
        boolean timeRangeApplied,
        int historyCount,
        int ragResultCount,
        int promptChars
) {
}
