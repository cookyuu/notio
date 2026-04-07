import 'package:notio_app/features/chat/data/models/chat_message_model.dart';
import 'package:notio_app/features/chat/data/models/daily_summary_model.dart';

/// Mock chat data for development
class ChatMockData {
  static int _nextId = 1;

  /// Get mock chat messages
  static List<ChatMessageModel> getMockMessages() {
    final now = DateTime.now();
    return [
      ChatMessageModel(
        id: 1,
        role: 'user',
        content: '안녕하세요! Notio 프로젝트의 진행 상황을 알려주세요.',
        createdAt: now.subtract(const Duration(hours: 2)).toIso8601String(),
      ),
      ChatMessageModel(
        id: 2,
        role: 'assistant',
        content:
            '안녕하세요! 현재 Notio 프로젝트는 Phase 1을 완료하고 Phase 2 AI 채팅 기능을 구현 중입니다.\n\n'
            '✅ **완료된 작업:**\n'
            '- 인증 시스템 (로그인/로그아웃)\n'
            '- 알림 탭 (목록, 필터링, 읽음 처리)\n'
            '- 기본 UI/UX 프레임워크\n\n'
            '🔄 **진행 중:**\n'
            '- AI 채팅 탭 구현\n'
            '- 더미 데이터 기반 프로토타입',
        createdAt: now.subtract(const Duration(hours: 2)).toIso8601String(),
      ),
      ChatMessageModel(
        id: 3,
        role: 'user',
        content: '오늘 받은 알림 중에 중요한 것들이 뭐가 있나요?',
        createdAt: now.subtract(const Duration(minutes: 30)).toIso8601String(),
      ),
      ChatMessageModel(
        id: 4,
        role: 'assistant',
        content:
            '오늘 받은 알림을 확인한 결과, 다음과 같은 중요한 알림이 있습니다:\n\n'
            '🔴 **높은 우선순위:**\n'
            '1. #dev-team 채널에서 PR #456 리뷰 요청 (5초 전)\n'
            '   - 박철수님이 멘션하셨습니다\n\n'
            '알림 탭에서 자세한 내용을 확인하실 수 있습니다.',
        createdAt: now.subtract(const Duration(minutes: 30)).toIso8601String(),
      ),
    ];
  }

  /// Get mock daily summary
  static DailySummaryModel getMockDailySummary() {
    return DailySummaryModel(
      summary:
          '오늘은 총 4개의 대화를 나누었습니다. 주요 주제는 Notio 프로젝트 진행 상황과 알림 관리였습니다. '
          'Phase 2 AI 채팅 기능 구현이 진행 중이며, 알림 시스템이 정상적으로 작동하고 있습니다.',
      date: DateTime.now().toIso8601String().split('T')[0],
      totalMessages: 4,
      topics: [
        '프로젝트 진행 상황',
        '알림 관리',
        'Phase 2 개발',
      ],
    );
  }

  /// Generate mock AI response
  static ChatMessageModel generateMockResponse(String userMessage) {
    final responses = [
      '알겠습니다. 요청하신 내용을 확인했습니다.',
      '네, 도움이 되었다면 좋겠습니다. 추가로 궁금하신 점이 있으신가요?',
      '좋은 질문입니다! 그 부분은 다음과 같이 설명할 수 있습니다.',
      '이해하셨습니다. 관련해서 더 자세한 정보를 제공해드리겠습니다.',
      '현재 상황을 분석한 결과, 다음과 같은 제안을 드립니다.',
    ];

    // Simple response selection based on message length
    final responseIndex = userMessage.length % responses.length;

    return ChatMessageModel(
      id: _nextId++,
      role: 'assistant',
      content: responses[responseIndex],
      createdAt: DateTime.now().toIso8601String(),
    );
  }

  /// Generate streaming response chunks
  static List<String> generateStreamingChunks(String fullResponse) {
    final words = fullResponse.split(' ');
    final chunks = <String>[];

    for (var i = 0; i < words.length; i++) {
      chunks.add('${words[i]} ');
    }

    return chunks;
  }
}
