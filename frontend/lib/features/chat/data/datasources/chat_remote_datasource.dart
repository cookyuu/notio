import 'package:dio/dio.dart';
import 'package:notio_app/features/chat/data/datasources/chat_mock_data.dart';
import 'package:notio_app/features/chat/data/models/chat_message_model.dart';
import 'package:notio_app/features/chat/data/models/chat_request.dart';
import 'package:notio_app/features/chat/data/models/daily_summary_model.dart';

/// Remote data source for chat messages
class ChatRemoteDataSource {
  // final Dio _dio;  // TODO: Uncomment when backend API is ready

  ChatRemoteDataSource(Dio dio);

  /// Send chat message and get response
  /// TODO: Replace with actual API endpoint when backend is ready
  Future<ChatMessageModel> sendMessage(ChatRequest request) async {
    // TODO: Uncomment when backend API is ready
    // try {
    //   final response = await _dio.post(
    //     '/api/v1/chat',
    //     data: request.toJson(),
    //   );
    //
    //   if (response.data['success'] == true) {
    //     return ChatMessageModel.fromJson(response.data['data']['message']);
    //   } else {
    //     throw Exception(response.data['error']['message']);
    //   }
    // } on DioException catch (e) {
    //   throw Exception('네트워크 오류: ${e.message}');
    // }

    // Mock implementation
    await Future.delayed(const Duration(milliseconds: 1000));
    return ChatMockData.generateMockResponse(request.content);
  }

  /// Stream chat message response (SSE)
  /// TODO: Replace with actual SSE endpoint when backend is ready
  Stream<String> streamMessage(ChatRequest request) async* {
    // TODO: Implement SSE streaming when backend is ready
    // try {
    //   final response = await _dio.get(
    //     '/api/v1/chat/stream',
    //     queryParameters: {'content': request.content},
    //     options: Options(
    //       responseType: ResponseType.stream,
    //       headers: {'Accept': 'text/event-stream'},
    //     ),
    //   );
    //
    //   final stream = response.data.stream;
    //   await for (final chunk in stream) {
    //     yield chunk;
    //   }
    // } on DioException catch (e) {
    //   throw Exception('네트워크 오류: ${e.message}');
    // }

    // Mock streaming implementation
    final mockResponse = ChatMockData.generateMockResponse(request.content);
    final chunks = ChatMockData.generateStreamingChunks(mockResponse.content);

    for (final chunk in chunks) {
      await Future.delayed(const Duration(milliseconds: 50));
      yield chunk;
    }
  }

  /// Get daily summary
  /// TODO: Replace with actual API endpoint when backend is ready
  Future<DailySummaryModel> getDailySummary() async {
    // TODO: Uncomment when backend API is ready
    // try {
    //   final response = await _dio.get('/api/v1/chat/daily-summary');
    //
    //   if (response.data['success'] == true) {
    //     return DailySummaryModel.fromJson(response.data['data']);
    //   } else {
    //     throw Exception(response.data['error']['message']);
    //   }
    // } on DioException catch (e) {
    //   throw Exception('네트워크 오류: ${e.message}');
    // }

    // Mock implementation
    await Future.delayed(const Duration(milliseconds: 500));
    return ChatMockData.getMockDailySummary();
  }

  /// Fetch chat history with pagination
  /// TODO: Replace with actual API endpoint when backend is ready
  Future<List<ChatMessageModel>> fetchHistory({
    int page = 0,
    int size = 20,
  }) async {
    // TODO: Uncomment when backend API is ready
    // try {
    //   final response = await _dio.get(
    //     '/api/v1/chat/history',
    //     queryParameters: {
    //       'page': page,
    //       'size': size,
    //     },
    //   );
    //
    //   if (response.data['success'] == true) {
    //     final List<dynamic> data = response.data['data'];
    //     return data.map((json) => ChatMessageModel.fromJson(json)).toList();
    //   } else {
    //     throw Exception(response.data['error']['message']);
    //   }
    // } on DioException catch (e) {
    //   throw Exception('네트워크 오류: ${e.message}');
    // }

    // Mock implementation - return mock messages only on first page
    await Future.delayed(const Duration(milliseconds: 600));
    if (page == 0) {
      return ChatMockData.getMockMessages();
    }
    return [];
  }
}
