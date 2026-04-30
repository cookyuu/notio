package com.notio.common.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NotioMetricsTagPolicy {

    private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
            "correlation_id",
            "user_id",
            "stream_id"
    );

    private static final Set<String> ALLOWED_TAG_KEYS = Set.of(
            "route",
            "provider",
            "source",
            "mode",
            "outcome",
            "exception",
            "time_range_applied"
    );

    public Tags sanitize(final Iterable<Tag> tags) {
        final Set<Tag> sanitized = new LinkedHashSet<>();
        for (final Tag tag : tags) {
            if (FORBIDDEN_TAG_KEYS.contains(tag.getKey())) {
                continue;
            }
            if (!ALLOWED_TAG_KEYS.contains(tag.getKey())) {
                continue;
            }
            sanitized.add(tag);
        }
        return Tags.of(List.copyOf(sanitized));
    }
}
