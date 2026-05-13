import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/services/sse/sse_client.dart';
import 'package:notio_app/features/notification/presentation/providers/notifications_notifier.dart';
import 'package:notio_app/shared/constant/api_constants.dart';

class RealtimeNotificationService {
  final Ref _ref;
  final SseClient _client = SseClient();

  RealtimeNotificationService(this._ref);

  void connect() {
    if (!kIsWeb) return;
    final url =
        '${ApiConstants.baseUrl}${ApiConstants.apiVersion}/notifications/stream';
    _client.connect(url, _onMessage);
  }

  void _onMessage() {
    _ref.invalidate(notificationsProvider);
    _ref.invalidate(unreadCountProvider);
  }

  void disconnect() {
    _client.disconnect();
  }
}

final realtimeNotificationServiceProvider =
    Provider<RealtimeNotificationService>((ref) {
  final service = RealtimeNotificationService(ref);
  service.connect();
  ref.onDispose(service.disconnect);
  return service;
});
