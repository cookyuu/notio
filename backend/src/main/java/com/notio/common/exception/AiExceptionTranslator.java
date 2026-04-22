package com.notio.common.exception;

import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
public class AiExceptionTranslator {

    public NotioException llmUnavailable(final Throwable cause) {
        return new NotioException(ErrorCode.LLM_UNAVAILABLE, ErrorCode.LLM_UNAVAILABLE.getMessage(), null, cause);
    }

    public NotioException embeddingFailed(final Throwable cause) {
        if (isConnectionFailure(cause) || isModelUnavailable(cause)) {
            return llmUnavailable(cause);
        }
        return new NotioException(ErrorCode.EMBEDDING_FAILED, ErrorCode.EMBEDDING_FAILED.getMessage(), null, cause);
    }

    private boolean isConnectionFailure(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResourceAccessException || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isModelUnavailable(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            final String message = current.getMessage();
            if (message != null && message.contains("model") && message.contains("not found")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
