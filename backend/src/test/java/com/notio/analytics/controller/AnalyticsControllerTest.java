package com.notio.analytics.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notio.analytics.dto.AiUsageGranularity;
import com.notio.analytics.dto.AiUsageResponse;
import com.notio.analytics.dto.WeeklyAnalyticsResponse;
import com.notio.analytics.service.AiUsageLogService;
import com.notio.analytics.service.AnalyticsService;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.GlobalExceptionHandler;
import com.notio.common.exception.NotioException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private AiUsageLogService aiUsageLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalyticsController(analyticsService, aiUsageLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void weeklyReturnsSummaryForAuthenticatedUser() throws Exception {
        when(analyticsService.getWeeklySummary(10L)).thenReturn(new WeeklyAnalyticsResponse(
                3,
                2,
                Map.of("GITHUB", 2L),
                Map.of("HIGH", 2L),
                Map.of("2026-04-17", 3L),
                "가장 많은 알림은 GITHUB 소스에서 발생했습니다."
        ));

        mockMvc.perform(get("/api/v1/analytics/weekly")
                        .principal(new UsernamePasswordAuthenticationToken("10", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNotifications").value(3))
                .andExpect(jsonPath("$.data.unreadNotifications").value(2))
                .andExpect(jsonPath("$.data.sourceDistribution.GITHUB").value(2));

        verify(analyticsService).getWeeklySummary(10L);
    }

    @Test
    void weeklyUsesPrincipalSpecificUserId() throws Exception {
        when(analyticsService.getWeeklySummary(10L)).thenReturn(new WeeklyAnalyticsResponse(
                1,
                1,
                Map.of("SLACK", 1L),
                Map.of("LOW", 1L),
                Map.of("2026-04-17", 1L),
                "가장 많은 알림은 SLACK 소스에서 발생했습니다."
        ));
        when(analyticsService.getWeeklySummary(20L)).thenReturn(new WeeklyAnalyticsResponse(
                4,
                0,
                Map.of("GITHUB", 4L),
                Map.of("HIGH", 4L),
                Map.of("2026-04-17", 4L),
                "가장 많은 알림은 GITHUB 소스에서 발생했습니다."
        ));

        mockMvc.perform(get("/api/v1/analytics/weekly")
                        .principal(new UsernamePasswordAuthenticationToken("10", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNotifications").value(1));

        mockMvc.perform(get("/api/v1/analytics/weekly")
                        .principal(new UsernamePasswordAuthenticationToken("20", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNotifications").value(4));

        verify(analyticsService).getWeeklySummary(10L);
        verify(analyticsService).getWeeklySummary(20L);
    }

    @Test
    void weeklyReturnsUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/weekly"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void weeklyReturnsUnauthorizedWhenPrincipalCannotBeParsed() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/weekly")
                        .principal(new UsernamePasswordAuthenticationToken("abc", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void aiUsageReturnsOkWithDailyDefaultWhenNoParams() throws Exception {
        final LocalDate today = LocalDate.now();
        final AiUsageResponse stub = new AiUsageResponse(
                AiUsageGranularity.DAILY, today.minusDays(6), today,
                0L, 0L, 0L, null, List.of(), List.of()
        );
        when(aiUsageLogService.getAiUsage(10L, AiUsageGranularity.DAILY, null, null)).thenReturn(stub);

        mockMvc.perform(get("/api/v1/analytics/ai-usage")
                        .principal(new UsernamePasswordAuthenticationToken("10", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void aiUsageReturnsBadRequestWhenGranularityIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/ai-usage")
                        .param("granularity", "INVALID")
                        .principal(new UsernamePasswordAuthenticationToken("10", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void aiUsageReturnsBadRequestWhenDateRangeExceedsMaxDays() throws Exception {
        final LocalDate startDate = LocalDate.of(2026, 1, 1);
        final LocalDate endDate = LocalDate.of(2026, 12, 31);
        when(aiUsageLogService.getAiUsage(10L, AiUsageGranularity.DAILY, startDate, endDate))
                .thenThrow(new NotioException(ErrorCode.INVALID_REQUEST, "조회 범위가 초과했습니다."));

        mockMvc.perform(get("/api/v1/analytics/ai-usage")
                        .param("granularity", "DAILY")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31")
                        .principal(new UsernamePasswordAuthenticationToken("10", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}
