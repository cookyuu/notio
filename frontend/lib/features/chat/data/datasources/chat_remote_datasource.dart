import 'package:dio/dio.dart';
import 'package:notio_app/features/chat/data/models/chat_message_model.dart';
import 'package:notio_app/features/chat/data/models/chat_request.dart';
import 'package:notio_app/features/chat/data/models/daily_summary_model.dart';

/// Remote data source for chat messages
class ChatRemoteDataSource {
  final Dio _dio;

  ChatRemoteDataSource(this._dio);

  /// Send chat message and get response
  Future<ChatMessageModel> sendMessage(ChatRequest request) async {
    try {
      final response = await _dio.post(
        '/api/v1/chat',
        data: request.toJson(),
        options: Options(
          // Set timeout longer than backend LLM timeout (30s) to avoid race condition
          receiveTimeout: const Duration(seconds: 35),
        ),
      );

      if (response.data['success'] == true) {
        return ChatMessageModel.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Stream chat message response (SSE)
  Stream<String> streamMessage(ChatRequest request) async* {
    try {
      final response = await _dio.get(
        '/api/v1/chat/stream',
        queryParameters: {'content': request.content},
        options: Options(
          responseType: ResponseType.stream,
          headers: {'Accept': 'text/event-stream'},
          // Set longer timeout for SSE streaming (backend LLM timeout is 30s)
          receiveTimeout: const Duration(seconds: 60),
        ),
      );

      final stream = response.data.stream;
      var pending = '';
      await for (final chunk in stream) {
        pending += String.fromCharCodes(chunk)
            .replaceAll('\r\n', '\n')
            .replaceAll('\r', '\n');
        var separatorIndex = pending.indexOf('\n\n');
        while (separatorIndex >= 0) {
          final eventBlock = pending.substring(0, separatorIndex);
          pending = pending.substring(separatorIndex + 2);
          final data = _extractChunkData(eventBlock);
          if (data != null && data.isNotEmpty) {
            yield data;
          }
          separatorIndex = pending.indexOf('\n\n');
        }
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  String? _extractChunkData(String eventBlock) {
    String? eventName;
    final dataLines = <String>[];

    for (final line in eventBlock.split('\n')) {
      if (line.startsWith('event:')) {
        eventName = line.substring(6).trim();
      } else if (line.startsWith('data:')) {
        dataLines.add(line.substring(5).trim());
      }
    }

    if (eventName != 'chunk') {
      return null;
    }

    return dataLines.join('\n');
  }

  /// Get daily summary
  Future<DailySummaryModel> getDailySummary() async {
    try {
      final response = await _dio.get('/api/v1/chat/daily-summary');

      if (response.data['success'] == true) {
        return DailySummaryModel.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }

  /// Fetch chat history with pagination
  Future<List<ChatMessageModel>> fetchHistory({
    int page = 0,
    int size = 20,
  }) async {
    try {
      final response = await _dio.get(
        '/api/v1/chat/history',
        queryParameters: {
          'page': page,
          'size': size,
        },
      );

      if (response.data['success'] == true) {
        final List<dynamic> data = response.data['data'];
        return data.map((json) => ChatMessageModel.fromJson(json)).toList();
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
  }
}
