import 'dart:async';
import 'dart:html' as html;

class SseClient {
  html.EventSource? _eventSource;
  StreamSubscription? _subscription;

  void connect(String url, void Function() onMessage) {
    _eventSource = html.EventSource(url, withCredentials: true);
    _subscription = _eventSource!.onMessage.listen((_) => onMessage());
  }

  void disconnect() {
    _subscription?.cancel();
    _eventSource?.close();
  }
}
