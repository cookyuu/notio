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
        ),
      );

      final stream = response.data.stream;
      await for (final chunk in stream) {
        final text = String.fromCharCodes(chunk);
        if (text.startsWith('data: ')) {
          yield text.substring(6).trim();
        }
      }
    } on DioException catch (e) {
      throw Exception('네트워크 오류: ${e.message}');
    }
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
