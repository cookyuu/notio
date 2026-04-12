package com.notio.chat.application;

import com.notio.chat.api.ChatMessageResponse;
import com.notio.chat.api.ChatRequest;
import com.notio.notification.api.NotificationFilterRequest;
import com.notio.notification.application.NotificationService;
import com.notio.notification.domain.Notification;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
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
        append("user", request.content());
        final String responseText = generateDummyAiResponse(request.content());
        return append("assistant", responseText);
    }

    public SseEmitter streamChat(final ChatRequest request) {
        append("user", request.content());
        final String responseText = generateDummyAiResponse(request.content());
        final SseEmitter emitter = new SseEmitter(30_000L);

        new Thread(() -> {
            try {
                final String[] chunks = splitIntoChunks(responseText);
                for (final String chunk : chunks) {
                    emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    Thread.sleep(50);
                }
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                append("assistant", responseText);
                emitter.complete();
            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        }).start();

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

    private String generateDummyAiResponse(final String userMessage) {
        final List<Notification> notifications = notificationService.findAll(
                null, // source
                null, // isRead
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        if (notifications.isEmpty()) {
            return "현재 수집된 알림이 없습니다. Webhook을 통해 알림이 들어오면 분석해드리겠습니다.";
        }

        final Map<NotificationSource, Long> sourceCount = notifications.stream()
                .collect(Collectors.groupingBy(Notification::getSource, Collectors.counting()));

        final Map<NotificationPriority, Long> priorityCount = notifications.stream()
                .collect(Collectors.groupingBy(Notification::getPriority, Collectors.counting()));

        final long unreadCount = notifications.stream()
                .filter(notification -> !notification.isRead())
                .count();

        final StringBuilder response = new StringBuilder();

        if (userMessage.contains("요약") || userMessage.contains("summary")) {
            response.append("최근 알림 분석 결과를 요약해드립니다.\n\n");
            response.append(String.format("총 %d건의 알림이 있으며, 그 중 %d건이 미읽음 상태입니다.\n\n",
                    notifications.size(), unreadCount));

            response.append("소스별 분포:\n");
            sourceCount.forEach((source, count) ->
                    response.append(String.format("- %s: %d건\n", source.name(), count)));

            response.append("\n우선순위별 분포:\n");
            priorityCount.forEach((priority, count) ->
                    response.append(String.format("- %s: %d건\n", priority.name(), count)));

            if (priorityCount.getOrDefault(NotificationPriority.HIGH, 0L) > 0) {
                response.append("\n⚠️ 높은 우선순위 알림이 있습니다. 확인해주세요!");
            }
        } else if (userMessage.contains("중요") || userMessage.contains("우선순위")) {
            final List<Notification> highPriority = notifications.stream()
                    .filter(notification -> notification.getPriority() == NotificationPriority.HIGH)
                    .limit(3)
                    .toList();

            if (highPriority.isEmpty()) {
                response.append("현재 높은 우선순위 알림이 없습니다.");
            } else {
                response.append(String.format("높은 우선순위 알림 %d건:\n\n", highPriority.size()));
                highPriority.forEach(notification ->
                        response.append(String.format("- [%s] %s\n",
                                notification.getSource().name(), notification.getTitle())));
            }
        } else if (userMessage.contains("오늘") || userMessage.contains("today")) {
            final LocalDate today = LocalDate.now(ZoneOffset.UTC);
            final long todayCount = notifications.stream()
                    .filter(notification -> notification.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate()
                            .equals(today))
                    .count();

            response.append(String.format("오늘 수집된 알림은 총 %d건입니다.\n\n", todayCount));

            if (todayCount > 0) {
                response.append("주요 내용:\n");
                notifications.stream()
                        .filter(notification -> notification.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate()
                                .equals(today))
                        .limit(3)
                        .forEach(notification ->
                                response.append(String.format("- %s\n", notification.getTitle())));
            }
        } else {
            response.append("안녕하세요! 알림 관리를 도와드리는 AI 어시스턴트입니다.\n\n");
            response.append(String.format("현재 총 %d건의 알림이 있습니다.\n", notifications.size()));
            response.append("다음과 같은 질문을 해보세요:\n");
            response.append("- \"오늘 알림 요약해줘\"\n");
            response.append("- \"중요한 알림 보여줘\"\n");
            response.append("- \"전체 요약해줘\"\n");
        }

        return response.toString();
    }

    private String[] splitIntoChunks(final String text) {
        final List<String> chunks = new ArrayList<>();
        final String[] words = text.split("\\s+");

        for (int i = 0; i < words.length; i += 3) {
            final StringBuilder chunk = new StringBuilder();
            for (int j = i; j < Math.min(i + 3, words.length); j++) {
                chunk.append(words[j]).append(" ");
            }
            chunks.add(chunk.toString().trim());
        }

        return chunks.toArray(new String[0]);
    }
}

