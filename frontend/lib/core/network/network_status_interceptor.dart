import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/network/network_status_provider.dart';

/// Interceptor that updates network status based on request/response
class NetworkStatusInterceptor extends Interceptor {
  final Ref ref;

  NetworkStatusInterceptor(this.ref);

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    // Successful response means we're online
    ref.read(networkStatusProvider.notifier).state = NetworkStatus.online;
    super.onResponse(response, handler);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    // Check if error is network-related
    if (err.type == DioExceptionType.connectionTimeout ||
        err.type == DioExceptionType.sendTimeout ||
        err.type == DioExceptionType.receiveTimeout ||
        err.type == DioExceptionType.connectionError) {
      // Network error means we're likely offline
      ref.read(networkStatusProvider.notifier).state = NetworkStatus.offline;
    }
    super.onError(err, handler);
  }
}
