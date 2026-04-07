import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/chat/data/datasources/chat_mock_data.dart';

void main() {
  group('ChatMockData', () {
    test('getMockMessages should return list of messages', () {
      // Act
      final messages = ChatMockData.getMockMessages();

      // Assert
      expect(messages, isNotEmpty);
      expect(messages.length, 4);
      expect(messages[0].role, 'user');
      expect(messages[1].role, 'assistant');
    });

    test('getMockDailySummary should return summary', () {
      // Act
      final summary = ChatMockData.getMockDailySummary();

      // Assert
      expect(summary.summary, isNotEmpty);
      expect(summary.totalMessages, 4);
      expect(summary.topics, isNotEmpty);
      expect(summary.date, isNotEmpty);
    });

    test('generateMockResponse should return assistant message', () {
      // Act
      final response = ChatMockData.generateMockResponse('Test message');

      // Assert
      expect(response.role, 'assistant');
      expect(response.content, isNotEmpty);
    });

    test('generateStreamingChunks should split response into chunks', () {
      // Arrange
      const fullResponse = 'This is a test response';

      // Act
      final chunks = ChatMockData.generateStreamingChunks(fullResponse);

      // Assert
      expect(chunks, isNotEmpty);
      expect(chunks.join('').trim(), fullResponse);
    });
  });
}
