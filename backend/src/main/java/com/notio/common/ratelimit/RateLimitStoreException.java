package com.notio.common.ratelimit;

public class RateLimitStoreException extends RuntimeException {

    public RateLimitStoreException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
