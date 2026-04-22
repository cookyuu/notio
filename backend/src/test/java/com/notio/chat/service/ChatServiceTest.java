package com.notio.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.chat.domain.ChatMessage;
import com.notio.chat.domain.ChatMessageRole;
import com.notio.chat.dto.ChatMessageResponse;
import com.notio.chat.repository.ChatMessageRepository;
import com.notio.notification.service.NotificationService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class ChatServiceTest {

    @Test
    void historyReadsRecentMessagesFromRepository() {
        final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
        final NotificationService notificationService = mock(NotificationService.class);
        final ChatService chatService = new ChatService(chatMessageRepository, notificationService);
        final ChatMessage message = message(
                10L,
                ChatMessageRole.ASSISTANT,
                "최근 알림 요약입니다.",
                OffsetDateTime.of(2026, 4, 22, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(chatMessageRepository.findRecentByUserId(eq(1L), pageableCaptor.capture()))
                .thenReturn(List.of(message));

        final List<ChatMessageResponse> history = chatService.history();

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().id()).isEqualTo(10L);
        assertThat(history.getFirst().role()).isEqualTo("ASSISTANT");
        assertThat(history.getFirst().content()).isEqualTo("최근 알림 요약입니다.");
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        verify(chatMessageRepository).findRecentByUserId(eq(1L), pageableCaptor.getValue());
    }

    private ChatMessage message(
            final Long id,
            final ChatMessageRole role,
            final String content,
            final OffsetDateTime createdAt
    ) {
        final ChatMessage message = new ChatMessage(1L, role, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        ReflectionTestUtils.setField(message, "updatedAt", createdAt);
        return message;
    }
}
