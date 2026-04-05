import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

/// Interceptor for logging HTTP requests and responses
class LoggingInterceptor extends Interceptor {
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    if (kDebugMode) {
      print('┌─────────────────────────────────────────────────────────────');
      print('│ REQUEST: ${options.method} ${options.uri}');
      print('│ Headers: ${options.headers}');
      if (options.data != null) {
        print('│ Body: ${options.data}');
      }
      print('└─────────────────────────────────────────────────────────────');
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    if (kDebugMode) {
      print('┌─────────────────────────────────────────────────────────────');
      print('│ RESPONSE: ${response.statusCode} ${response.requestOptions.uri}');
      print('│ Data: ${response.data}');
      print('└─────────────────────────────────────────────────────────────');
    }
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (kDebugMode) {
      print('┌─────────────────────────────────────────────────────────────');
      print('│ ERROR: ${err.requestOptions.method} ${err.requestOptions.uri}');
      print('│ Message: ${err.message}');
      print('│ Response: ${err.response?.data}');
      print('└─────────────────────────────────────────────────────────────');
    }
    handler.next(err);
  }
}
