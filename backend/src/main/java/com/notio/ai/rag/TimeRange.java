package com.notio.ai.rag;

import java.time.Instant;

public record TimeRange(
        Instant startInclusive,
        Instant endExclusive
) {
}
