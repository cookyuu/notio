package com.notio.chat.application;

import com.notio.chat.api.ChatMessageResponse;
import com.notio.chat.api.ChatRequest;
import com.notio.notification.api.NotificationFilterRequest;
import com.notio.notification.application.NotificationService;
import com.notio.notification.domain.Notification;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatService {

    private final List<ChatMessageResponse> history = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(0);
    private final NotificationService notificationService;

    public ChatService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public ChatMessageResponse chat(final ChatRequest request) {
        append("user", request.message());
        final List<Notification> notifications = notificationService.findAll(
                new NotificationFilterRequest(null, null, null, null),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        final String responseText = notifications.isEmpty()
                ? "최근 알림이 없습니다. 새 webhook 이벤트를 기다리고 있습니다."
                : "최근 알림 " + notifications.size() + "건 기준 요약입니다: "
                + notifications.stream().map(Notification::getTitle).toList();
        return append("assistant", responseText);
    }

    public SseEmitter streamChat(final ChatRequest request) {
        final ChatMessageResponse response = chat(request);
        final SseEmitter emitter = new SseEmitter(5_000L);
        try {
            emitter.send(SseEmitter.event().name("message").data(response));
            emitter.complete();
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public List<ChatMessageResponse> history() {
        return List.copyOf(history);
    }

    private ChatMessageResponse append(final String role, final String content) {
        final ChatMessageResponse message = new ChatMessageResponse(
                sequence.incrementAndGet(),
                role,
                content,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        history.add(0, message);
        return message;
    }
}

