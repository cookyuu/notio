package com.notio.chat.service;

import com.notio.ai.rag.TimeRange;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ChatTimeRangeExtractor {

    private static final Pattern RELATIVE_RANGE_PATTERN = Pattern.compile("(최근|지난)\\s*(\\d+)\\s*(분|시간|일)");
    private static final Pattern UNSUPPORTED_DATE_RANGE_PATTERN = Pattern.compile("(부터|까지|~|～)");

    private final Clock clock;

    public ChatTimeRangeExtractor(final Clock clock) {
        this.clock = clock;
    }

    public Optional<TimeRange> extract(final String question) {
        if (question == null || question.isBlank()) {
            return Optional.empty();
        }

        final Optional<TimeRange> relativeRange = extractRelativeRange(question);
        if (relativeRange.isPresent()) {
            return relativeRange;
        }

        if (UNSUPPORTED_DATE_RANGE_PATTERN.matcher(question).find()) {
            return Optional.empty();
        }

        if (question.contains("오늘")) {
            return Optional.of(dayRange(LocalDate.now(clock)));
        }

        if (question.contains("어제")) {
            return Optional.of(dayRange(LocalDate.now(clock).minusDays(1)));
        }

        return Optional.empty();
    }

    private Optional<TimeRange> extractRelativeRange(final String question) {
        final Matcher matcher = RELATIVE_RANGE_PATTERN.matcher(question);
        if (!matcher.find()) {
            return Optional.empty();
        }

        final long amount;
        try {
            amount = Long.parseLong(matcher.group(2));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }

        final Duration duration = switch (matcher.group(3)) {
            case "분" -> Duration.ofMinutes(amount);
            case "시간" -> Duration.ofHours(amount);
            case "일" -> Duration.ofDays(amount);
            default -> null;
        };

        if (duration == null) {
            return Optional.empty();
        }

        final Instant now = clock.instant();
        return Optional.of(new TimeRange(now.minus(duration), now));
    }

    private TimeRange dayRange(final LocalDate date) {
        final ZoneId zone = clock.getZone();
        return new TimeRange(
                date.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant()
        );
    }
}
