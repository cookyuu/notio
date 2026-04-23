// Phase 5 빈 결과 및 장애 상태 점검
//
// 검증 항목:
// - 빈 RAG 결과 fallback이 일반 assistant 메시지로 표시된다
// - 기간 파싱 실패 fallback이 error state로 표시되지 않는다
// - LLM/embedding 장애 시 에러가 assistant 메시지로 표시된다
// - Ollama 미기동/timeout 후 isSending, isStreaming이 false로 복원된다
// - SSE 연결 중간 끊김 후 상태 플래그가 정상 해제된다
// - retry 시 user message가 중복 저장되지 않는다

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/chat/data/models/daily_summary_model.dart';
import 'package:notio_app/features/chat/domain/entities/chat_message_entity.dart';
import 'package:notio_app/features/chat/domain/entities/message_role.dart';
import 'package:notio_app/features/chat/domain/repositories/chat_repository.dart';
import 'package:notio_app/features/chat/presentation/providers/chat_providers.dart';

class _FakeChatRepository implements ChatRepository {
  List<String> streamChunks = [];
  Exception? streamError;
  List<ChatMessageEntity> historyMessages = [];
  final List<ChatMessageEntity> _cache = [];

  @override
  Future<ChatMessageEntity> sendMessage(String content) async {
    return ChatMessageEntity(
      id: 99,
      role: MessageRole.assistant,
      content: 'ok',
      createdAt: DateTime.now(),
    );
  }

  @override
  Stream<String> streamMessage(String content) async* {
    for (final chunk in streamChunks) {
      yield chunk;
    }
    if (streamError != null) throw streamError!;
  }

  @override
  Future<List<ChatMessageEntity>> fetchHistory({
    int page = 0,
    int size = 20,
  }) async {
    return historyMessages;
  }

  @override
  Future<DailySummaryModel> getDailySummary() async {
    return const DailySummaryModel(
      summary: 'test summary',
      date: '2026-04-23',
      totalMessages: 0,
      topics: [],
    );
  }

  @override
  Future<List<ChatMessageEntity>> getCachedMessages() async =>
      List.from(_cache);

  @override
  void cacheMessages(List<ChatMessageEntity> messages) {
    _cache
      ..clear()
      ..addAll(messages);
  }

  @override
  void addMessageToCache(ChatMessageEntity message) => _cache.add(message);

  @override
  void clearCache() => _cache.clear();
}

void main() {
  late ProviderContainer container;
  late _FakeChatRepository repo;

  setUp(() async {
    repo = _FakeChatRepository();
    container = ProviderContainer(
      overrides: [
        chatRepositoryProvider.overrideWithValue(repo),
      ],
    );
    // Force notifier creation so _loadInitialMessages() starts immediately,
    // then drain the microtask queue so it completes before each test runs.
    container.read(chatProvider.notifier);
    await Future.delayed(Duration.zero);
  });

  tearDown(() {
    container.dispose();
  });

  group('Phase 5 - 빈 결과 및 장애 상태 점검', () {
    test('1. 빈 RAG 결과 fallback이 error state가 아닌 일반 assistant 메시지로 표시된다',
        () async {
      repo.streamChunks = ['이 기간에는 알림이 없습니다.'];

      await container
          .read(chatProvider.notifier)
          .sendMessageWithStreaming('최근 5시간 알림 요약해줘');

      final state = container.read(chatProvider);
      expect(state.error, isNull,
          reason: 'state.error는 null이어야 합니다 — error state가 아님');
      expect(state.isStreaming, isFalse);
      expect(state.isSending, isFalse);
      final assistantMsgs =
          state.messages.where((m) => m.role == MessageRole.assistant).toList();
      expect(assistantMsgs, isNotEmpty);
      expect(assistantMsgs.last.content, contains('이 기간에는 알림이 없습니다.'));
    });

    test('2. 기간 파싱 실패 fallback이 프론트 error state로 표시되지 않는다', () async {
      repo.streamChunks = ['기간을 인식하지 못했습니다. 일반 검색으로 대신 답변합니다.'];

      await container
          .read(chatProvider.notifier)
          .sendMessageWithStreaming('4월 20일부터 4월 22일까지 알림 요약해줘');

      final state = container.read(chatProvider);
      expect(state.error, isNull,
          reason: 'fallback은 error state가 아닌 assistant 메시지로 처리되어야 합니다');
      final assistantMsgs =
          state.messages.where((m) => m.role == MessageRole.assistant).toList();
      expect(assistantMsgs.last.content, contains('기간을 인식하지 못했습니다'));
    });

    test(
        '3. LLM 또는 embedding 장애 시 에러가 assistant 메시지로 표시되고 state.error는 설정되지 않는다',
        () async {
      repo.streamError = Exception('Ollama 서비스에 연결할 수 없습니다.');

      await container
          .read(chatProvider.notifier)
          .sendMessageWithStreaming('알림 요약해줘');

      final state = container.read(chatProvider);
      expect(state.error, isNull,
          reason: 'error는 assistant 메시지로 표시되고 state.error는 null이어야 합니다');
      final assistantMsgs =
          state.messages.where((m) => m.role == MessageRole.assistant).toList();
      expect(assistantMsgs, isNotEmpty,
          reason: '장애 내용이 assistant 메시지로 표시되어야 합니다');
      expect(assistantMsgs.last.content, contains('❌'));
    });

    test(
        '4. Ollama 미기동 또는 backend timeout 후 isSending, isStreaming이 false로 복원되어 입력창이 활성화된다',
        () async {
      repo.streamError = Exception('Connection refused');

      await container
          .read(chatProvider.notifier)
          .sendMessageWithStreaming('질문');

      final state = container.read(chatProvider);
      expect(state.isSending, isFalse,
          reason: 'isSending이 false여야 입력창이 활성화됩니다');
      expect(state.isStreaming, isFalse,
          reason: 'isStreaming이 false여야 입력창이 활성화됩니다');
    });

    test('5. SSE 연결이 중간에 끊겨도 isSending, isStreaming, streamingContent가 정상 해제된다',
        () async {
      repo.streamChunks = ['부분 응답'];
      repo.streamError = Exception('Connection reset by peer');

      await container
          .read(chatProvider.notifier)
          .sendMessageWithStreaming('질문');

      final state = container.read(chatProvider);
      expect(state.isSending, isFalse);
      expect(state.isStreaming, isFalse);
      expect(state.streamingContent, isNull);
    });

    test('6. retry 또는 재전송 시 user message가 의도치 않게 중복 저장되지 않는다', () async {
      // 1차 전송 — 에러
      repo.streamError = Exception('일시적 오류');
      await container
          .read(chatProvider.notifier)
          .sendMessageWithStreaming('첫 번째 메시지');

      // 2차 전송 — 성공
      repo
        ..streamError = null
        ..streamChunks = ['응답'];
      await container
          .read(chatProvider.notifier)
          .sendMessageWithStreaming('두 번째 메시지');

      final state = container.read(chatProvider);
      final userMsgs =
          state.messages.where((m) => m.role == MessageRole.user).toList();
      expect(userMsgs.length, equals(2),
          reason: 'user 메시지는 정확히 2개여야 합니다 (중복 없음)');
      expect(userMsgs[0].content, equals('첫 번째 메시지'));
      expect(userMsgs[1].content, equals('두 번째 메시지'));
    });
  });
}
