import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:notio_app/features/chat/data/models/chat_message_model.dart';
import 'package:notio_app/features/chat/data/models/chat_request.dart';
import 'package:notio_app/features/chat/data/models/daily_summary_model.dart';

/// Remote data source for chat messages
class ChatRemoteDataSource {
  static const chatReceiveTimeout = Duration(minutes: 2);
  static const summaryReceiveTimeout = Duration(minutes: 3);
  static const streamReceiveTimeout = Duration(minutes: 5);

  final Dio _dio;

  ChatRemoteDataSource(this._dio);

  /// Send chat message and get response
  Future<ChatMessageModel> sendMessage(ChatRequest request) async {
    try {
      final response = await _dio.post(
        '/api/v1/chat',
        data: request.toJson(),
        options: Options(
          receiveTimeout: chatReceiveTimeout,
        ),
      );

      if (response.data['success'] == true) {
        return ChatMessageModel.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw _dioError(e);
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
          receiveTimeout: streamReceiveTimeout,
        ),
      );

      var pending = '';
      final byteStream = response.data.stream.cast<List<int>>();
      await for (final text in utf8.decoder.bind(byteStream)) {
        pending += text.replaceAll('\r\n', '\n').replaceAll('\r', '\n');
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
      throw _dioError(e);
    }
  }

  String? _extractChunkData(String eventBlock) {
    for (final line in eventBlock.split('\n')) {
      if (line.startsWith('data:')) {
        final raw = line.substring(5).trim();
        try {
          final decoded = jsonDecode(raw) as Map<String, dynamic>;
          if (decoded.containsKey('chunk')) {
            return decoded['chunk'] as String?;
          }
          // 'done' payload: stream ends naturally, nothing to yield
        } catch (_) {
          // non-JSON raw text fallback
          if (raw.isNotEmpty) return raw;
        }
      }
    }
    return null;
  }

  /// Get daily summary
  Future<DailySummaryModel> getDailySummary() async {
    try {
      final response = await _dio.get(
        '/api/v1/chat/daily-summary',
        options: Options(
          receiveTimeout: summaryReceiveTimeout,
        ),
      );

      if (response.data['success'] == true) {
        return DailySummaryModel.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['error']['message']);
      }
    } on DioException catch (e) {
      throw _dioError(e);
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
      throw _dioError(e);
    }
  }

  /// Extracts the backend's JSON error message from a DioException when
  /// the server responded with a standard ApiResponse error body.
  Exception _dioError(DioException e) {
    String? serverMessage;
    final data = e.response?.data;
    if (data is Map) {
      serverMessage = data['error']?['message'] as String?;
    }
    return Exception(serverMessage ?? '네트워크 오류: ${e.message}');
  }
}
