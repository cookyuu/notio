package com.notio.chat.dto;

import java.time.LocalDate;
import java.util.List;

public record DailySummaryResponse(
        String summary,
        String date,
        int totalMessages,
        List<String> topics
) {
}

