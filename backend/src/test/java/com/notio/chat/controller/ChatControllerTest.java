package com.notio.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.notio.chat.dto.DailySummaryResponse;
import com.notio.chat.service.ChatService;
import com.notio.chat.service.DailySummaryService;
import com.notio.common.response.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatControllerTest {

    @Test
    void dailySummaryReturnsStringDatePayload() {
        final ChatService chatService = mock(ChatService.class);
        final DailySummaryService dailySummaryService = mock(DailySummaryService.class);
        final ChatController controller = new ChatController(chatService, dailySummaryService);

        when(dailySummaryService.getSummary()).thenReturn(
                new DailySummaryResponse(
                        "오늘 총 0건의 알림이 수집되었습니다.",
                        "2026-04-13",
                        0,
                        List.of()
                )
        );

        final ApiResponse<DailySummaryResponse> response = controller.dailySummary();

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isNotNull();
        assertThat(response.data().date()).isEqualTo("2026-04-13");
    }
}
