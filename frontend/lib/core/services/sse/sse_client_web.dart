import 'dart:async';

import 'package:web/web.dart' as web;

class SseClient {
  web.EventSource? _eventSource;
  StreamSubscription? _subscription;

  void connect(String url, void Function() onMessage) {
    _eventSource = web.EventSource(url, web.EventSourceInit(withCredentials: true));
    _subscription = _eventSource!.onMessage.listen((_) => onMessage());
  }

  void disconnect() {
    _subscription?.cancel();
    _eventSource?.close();
  }
}
