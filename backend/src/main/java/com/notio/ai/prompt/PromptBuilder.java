package com.notio.ai.prompt;

import com.notio.ai.rag.RagDocument;
import com.notio.notification.domain.Notification;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public LlmPrompt buildNotificationSummaryPrompt(
            Notification notification,
            List<RagDocument> ragContext) {

        String systemPrompt = """
                당신은 개발자 알림을 채팅 플랫폼(Slack/Telegram/Discord)용으로 간결하게 요약하는 AI입니다.
                규칙:
                1. 2~4문장으로 핵심 내용과 필요한 조치를 명확히 설명하세요.
                2. 마크다운 굵게(**)와 코드 인라인(`)을 적절히 활용하세요.
                3. 유사한 과거 알림이 있다면 "이전과 동일한 유형" 임을 언급하세요.
                4. 최대 500자를 초과하지 마세요.
                5. 불필요한 인사말, 부연 설명을 생략하세요.
                """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("## 알림 정보\n");
        userPrompt.append("- **소스**: ").append(notification.getSource()).append("\n");
        userPrompt.append("- **제목**: ").append(notification.getTitle()).append("\n");
        userPrompt.append("- **우선순위**: ").append(notification.getPriority()).append("\n");
        userPrompt.append("- **내용**:\n").append(notification.getBody()).append("\n");
        if (notification.getExternalUrl() != null) {
            userPrompt.append("- **링크**: ").append(notification.getExternalUrl()).append("\n");
        }

        if (!ragContext.isEmpty()) {
            userPrompt.append("\n## 유사 과거 알림 (참고용)\n");
            ragContext.stream().limit(3).forEach(doc ->
                userPrompt.append("- [").append(doc.source()).append("] ")
                          .append(doc.title()).append(" (유사도: ")
                          .append(String.format("%.2f", doc.similarityScore())).append(")\n")
            );
        }

        userPrompt.append("\n위 알림을 채팅 플랫폼 전달용으로 요약해주세요.");

        return new LlmPrompt(systemPrompt, userPrompt.toString());
    }

    public LlmPrompt buildDigestSummaryPrompt(List<Notification> notifications) {

        String systemPrompt = """
                당신은 여러 개발자 알림을 하나의 묶음 요약 메시지로 작성하는 AI입니다.
                규칙:
                1. 첫 줄: 전체 요약 1~2문장 (총 N개 알림, 주요 주제 포함).
                2. 이후: 각 알림을 1줄로 요약 (- [소스] 제목: 핵심 내용 형식).
                3. 마크다운 목록(-)을 사용하세요.
                4. 최대 1000자를 초과하지 마세요.
                """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("## 묶음 전달할 알림 목록 (").append(notifications.size()).append("개)\n\n");

        notifications.forEach(n -> {
            userPrompt.append("### [").append(n.getSource()).append("] ")
                      .append(n.getTitle()).append("\n");
            userPrompt.append("- 우선순위: ").append(n.getPriority()).append("\n");
            String body = n.getAiSummary() != null ? n.getAiSummary() : n.getBody();
            userPrompt.append("- 내용: ").append(truncate(body, 300)).append("\n\n");
        });

        userPrompt.append("위 알림들을 하나의 묶음 요약 메시지로 작성해주세요.");

        return new LlmPrompt(systemPrompt, userPrompt.toString());
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.isBlank()) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
