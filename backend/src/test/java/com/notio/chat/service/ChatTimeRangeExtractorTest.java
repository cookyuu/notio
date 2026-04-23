package com.notio.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.notio.ai.rag.TimeRange;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatTimeRangeExtractorTest {

    private static final Instant NOW = Instant.parse("2026-04-22T03:30:00Z");
    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

    private final ChatTimeRangeExtractor extractor = new ChatTimeRangeExtractor(
            Clock.fixed(NOW, SERVER_ZONE)
    );

    @Test
    void extractConvertsRecentFiveHoursToRangeEndingAtNow() {
        final Optional<TimeRange> result = extractor.extract("최근 5시간 내의 알림 내역을 요약해줘");

        assertThat(result).hasValue(new TimeRange(
                Instant.parse("2026-04-21T22:30:00Z"),
                NOW
        ));
    }

    @Test
    void extractConvertsPastThirtyMinutesToRangeEndingAtNow() {
        final Optional<TimeRange> result = extractor.extract("지난 30분 동안 중요한 알림 알려줘");

        assertThat(result).hasValue(new TimeRange(
                Instant.parse("2026-04-22T03:00:00Z"),
                NOW
        ));
    }

    @Test
    void extractConvertsRecentThreeDaysToRangeEndingAtNow() {
        final Optional<TimeRange> result = extractor.extract("최근 3일 알림 요약해줘");

        assertThat(result).hasValue(new TimeRange(
                Instant.parse("2026-04-19T03:30:00Z"),
                NOW
        ));
    }

    @Test
    void extractConvertsTodayToServerZoneDayRange() {
        final Optional<TimeRange> result = extractor.extract("오늘 받은 알림 요약해줘");

        assertThat(result).hasValue(new TimeRange(
                Instant.parse("2026-04-21T15:00:00Z"),
                Instant.parse("2026-04-22T15:00:00Z")
        ));
    }

    @Test
    void extractConvertsYesterdayToServerZoneDayRange() {
        final Optional<TimeRange> result = extractor.extract("어제 알림 정리해줘");

        assertThat(result).hasValue(new TimeRange(
                Instant.parse("2026-04-20T15:00:00Z"),
                Instant.parse("2026-04-21T15:00:00Z")
        ));
    }

    @Test
    void extractReturnsEmptyWhenQuestionHasNoTimeExpression() {
        final Optional<TimeRange> result = extractor.extract("중요한 알림 알려줘");

        assertThat(result).isEmpty();
    }

    @Test
    void extractReturnsEmptyForUnsupportedDateRangeExpression() {
        final Optional<TimeRange> result = extractor.extract("4월 1일부터 4월 3일까지 알림 요약해줘");

        assertThat(result).isEmpty();
    }

    @Test
    void extractPrefersRelativeRangeOverDateKeyword() {
        final Optional<TimeRange> result = extractor.extract("오늘 최근 5시간 알림 요약해줘");

        assertThat(result).hasValue(new TimeRange(
                Instant.parse("2026-04-21T22:30:00Z"),
                NOW
        ));
    }
}
