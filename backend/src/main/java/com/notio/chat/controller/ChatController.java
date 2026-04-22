package com.notio.chat.controller;

import com.notio.chat.dto.ChatMessageResponse;
import com.notio.chat.dto.ChatRequest;
import com.notio.chat.dto.DailySummaryResponse;
import com.notio.chat.service.ChatService;
import com.notio.chat.service.DailySummaryService;
import com.notio.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/chat")
@Validated
public class ChatController {

    private final ChatService chatService;
    private final DailySummaryService dailySummaryService;

    public ChatController(
            final ChatService chatService,
            final DailySummaryService dailySummaryService
    ) {
        this.chatService = chatService;
        this.dailySummaryService = dailySummaryService;
    }

    @PostMapping
    public ApiResponse<ChatMessageResponse> chat(@Valid @RequestBody final ChatRequest request) {
        return ApiResponse.success(chatService.chat(request));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(name = "content") @NotBlank final String content) {
        return chatService.streamChat(new ChatRequest(content));
    }

    @GetMapping("/daily-summary")
    public ApiResponse<DailySummaryResponse> dailySummary() {
        return ApiResponse.success(dailySummaryService.getSummary());
    }

    @GetMapping("/history")
    public ApiResponse<List<ChatMessageResponse>> history() {
        return ApiResponse.success(chatService.history());
    }
}
