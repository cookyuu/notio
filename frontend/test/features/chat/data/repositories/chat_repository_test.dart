import 'package:flutter_test/flutter_test.dart';
import 'package:dio/dio.dart';
import 'package:notio_app/features/chat/data/datasources/chat_local_datasource.dart';
import 'package:notio_app/features/chat/data/datasources/chat_remote_datasource.dart';
import 'package:notio_app/features/chat/data/repositories/chat_repository_impl.dart';
import 'package:notio_app/features/chat/domain/entities/message_role.dart';

void main() {
  group('ChatRepository', () {
    late ChatRepositoryImpl repository;
    late ChatRemoteDataSource remoteDataSource;
    late ChatLocalDataSource localDataSource;
    late Dio dio;

    setUp(() {
      dio = Dio();
      remoteDataSource = ChatRemoteDataSource(dio);
      localDataSource = ChatLocalDataSource();
      repository = ChatRepositoryImpl(
        remoteDataSource: remoteDataSource,
        localDataSource: localDataSource,
      );
    });

    test('sendMessage should return AI response', () async {
      // Act
      final response = await repository.sendMessage('Hello');

      // Assert
      expect(response.role, MessageRole.assistant);
      expect(response.content, isNotEmpty);
    });

    test('sendMessage should cache messages', () async {
      // Act
      await repository.sendMessage('Test message');

      // Assert
      final cachedMessages = repository.getCachedMessages();
      expect(cachedMessages.length, 2); // User message + AI response
      expect(cachedMessages[0].role, MessageRole.user);
      expect(cachedMessages[1].role, MessageRole.assistant);
    });

    test('fetchHistory should return mock messages', () async {
      // Act
      final messages = await repository.fetchHistory(page: 0);

      // Assert
      expect(messages, isNotEmpty);
      expect(messages.length, 4); // Mock data has 4 messages
    });

    test('fetchHistory should cache first page results', () async {
      // Act
      await repository.fetchHistory(page: 0);

      // Assert
      final cachedMessages = repository.getCachedMessages();
      expect(cachedMessages, isNotEmpty);
    });

    test('getDailySummary should return summary', () async {
      // Act
      final summary = await repository.getDailySummary();

      // Assert
      expect(summary.summary, isNotEmpty);
      expect(summary.totalMessages, greaterThan(0));
      expect(summary.topics, isNotEmpty);
    });

    test('clearCache should remove all cached messages', () async {
      // Arrange
      await repository.sendMessage('Test');

      // Act
      repository.clearCache();

      // Assert
      final cachedMessages = repository.getCachedMessages();
      expect(cachedMessages, isEmpty);
    });

    test('streamMessage should emit chunks', () async {
      // Act
      final stream = repository.streamMessage('Test');
      final chunks = await stream.toList();

      // Assert
      expect(chunks, isNotEmpty);
      expect(chunks.every((chunk) => chunk.isNotEmpty), isTrue);
    });
  });
}
