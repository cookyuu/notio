package com.notio.common.ratelimit;

public class PayloadTooLargeException extends RuntimeException {

    private final int maxBytes;

    public PayloadTooLargeException(final int maxBytes) {
        super("요청 본문 크기가 제한을 초과했습니다.");
        this.maxBytes = maxBytes;
    }

    public int getMaxBytes() {
        return maxBytes;
    }
}
