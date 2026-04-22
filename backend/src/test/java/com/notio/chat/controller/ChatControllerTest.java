package com.notio.chat.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notio.ai.llm.LlmProvider;
import com.notio.ai.prompt.LlmPrompt;
import com.notio.ai.prompt.PromptBuilder;
import com.notio.ai.rag.RagRetriever;
import com.notio.chat.domain.ChatMessage;
import com.notio.chat.domain.ChatMessageRole;
import com.notio.chat.dto.ChatMessageResponse;
import com.notio.chat.dto.DailySummaryResponse;
import com.notio.chat.repository.ChatMessageRepository;
import com.notio.chat.service.ChatService;
import com.notio.chat.service.DailySummaryService;
import com.notio.common.config.JacksonConfig;
import com.notio.common.config.properties.NotioAiProperties;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatControllerTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    @Test
    void chatReturnsApiResponseWithChatMessageContract() throws Exception {
        final ChatService chatService = mock(ChatService.class);
        final DailySummaryService dailySummaryService = mock(DailySummaryService.class);
        final MockMvc mockMvc = mockMvc(new ChatController(chatService, dailySummaryService));

        when(chatService.chat(any())).thenReturn(new ChatMessageResponse(
                124L,
                "ASSISTANT",
                "GitHub PR 리뷰 요청이 중요합니다.",
                OffsetDateTime.of(2026, 4, 22, 10, 1, 0, 0, ZoneOffset.UTC)
        ));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"오늘 처리해야 할 중요한 알림을 알려줘\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(124))
                .andExpect(jsonPath("$.data.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.content").value("GitHub PR 리뷰 요청이 중요합니다."))
                .andExpect(jsonPath("$.data.created_at").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void dailySummaryReturnsStringDatePayload() throws Exception {
        final ChatService chatService = mock(ChatService.class);
        final DailySummaryService dailySummaryService = mock(DailySummaryService.class);
        final MockMvc mockMvc = mockMvc(new ChatController(chatService, dailySummaryService));

        when(dailySummaryService.getSummary()).thenReturn(
                new DailySummaryResponse(
                        "오늘 총 0건의 알림이 수집되었습니다.",
                        "2026-04-22",
                        0,
                        List.of()
                )
        );

        mockMvc.perform(get("/api/v1/chat/daily-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary").value("오늘 총 0건의 알림이 수집되었습니다."))
                .andExpect(jsonPath("$.data.date").value("2026-04-22"))
                .andExpect(jsonPath("$.data.total_messages").value(0))
                .andExpect(jsonPath("$.data.topics").isArray())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void historyReturnsListDataContract() throws Exception {
        final ChatService chatService = mock(ChatService.class);
        final DailySummaryService dailySummaryService = mock(DailySummaryService.class);
        final MockMvc mockMvc = mockMvc(new ChatController(chatService, dailySummaryService));

        when(chatService.history()).thenReturn(List.of(new ChatMessageResponse(
                101L,
                "USER",
                "오늘 중요한 알림 알려줘",
                OffsetDateTime.of(2026, 4, 22, 9, 59, 50, 0, ZoneOffset.UTC)
        )));

        mockMvc.perform(get("/api/v1/chat/history?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(101))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[0].created_at").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamReturnsJsonChunkAndDoneDataEvents() throws Exception {
        final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        final RagRetriever ragRetriever = mock(RagRetriever.class);
        final PromptBuilder promptBuilder = mock(PromptBuilder.class);
        final LlmProvider llmProvider = mock(LlmProvider.class);
        final DailySummaryService dailySummaryService = mock(DailySummaryService.class);
        final ChatService chatService = new ChatService(
                chatMessageRepository,
                ragRetriever,
                promptBuilder,
                llmProvider,
                new NotioAiProperties(Duration.ofSeconds(30), Duration.ofSeconds(15), Duration.ofSeconds(5)),
                objectMapper
        );
        final MockMvc mockMvc = mockMvc(new ChatController(chatService, dailySummaryService));
        final ChatMessage userMessage = message(1L, ChatMessageRole.USER, "오늘 중요한 알림 알려줘");
        final ChatMessage assistantMessage = message(2L, ChatMessageRole.ASSISTANT, "GitHub PR review requested");
        final LlmPrompt prompt = new LlmPrompt("system", "user");

        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenReturn(userMessage)
                .thenReturn(assistantMessage);
        when(ragRetriever.retrieve(1L, "오늘 중요한 알림 알려줘")).thenReturn(List.of());
        when(chatMessageRepository.findRecentByUserId(eq(1L), any(Pageable.class))).thenReturn(List.of(userMessage));
        when(promptBuilder.buildChatPrompt("오늘 중요한 알림 알려줘", List.of(), List.of(userMessage)))
                .thenReturn(prompt);
        org.mockito.Mockito.doAnswer(invocation -> {
            final Consumer<String> chunkConsumer = invocation.getArgument(1);
            chunkConsumer.accept("GitHub PR ");
            chunkConsumer.accept("review requested");
            return null;
        }).when(llmProvider).stream(eq(prompt), any(Consumer.class));

        final MvcResult mvcResult = mockMvc.perform(get("/api/v1/chat/stream")
                        .param("content", "오늘 중요한 알림 알려줘")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("\"chunk\":\"GitHub PR \"")))
                .andExpect(content().string(containsString("\"chunk\":\"review requested\"")))
                .andExpect(content().string(containsString("\"done\":true")))
                .andExpect(content().string(containsString("\"message_id\":2")))
                .andExpect(content().string(not(containsString("event:chunk"))))
                .andExpect(content().string(not(containsString("event:done"))));
    }

    private MockMvc mockMvc(final ChatController controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private ChatMessage message(final Long id, final ChatMessageRole role, final String content) {
        final OffsetDateTime createdAt = OffsetDateTime.of(2026, 4, 22, 10, 0, 0, 0, ZoneOffset.UTC);
        final ChatMessage message = new ChatMessage(1L, role, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        ReflectionTestUtils.setField(message, "updatedAt", createdAt);
        return message;
    }
}
