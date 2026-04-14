import 'dart:async';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'network_status_provider.dart';

/// Service for monitoring network connectivity
class ConnectivityService {
  final Connectivity _connectivity;
  final Ref _ref;
  StreamSubscription<List<ConnectivityResult>>? _subscription;

  ConnectivityService(this._connectivity, this._ref);

  /// Initialize connectivity monitoring
  Future<void> initialize() async {
    // Check initial connectivity
    final result = await _connectivity.checkConnectivity();
    _updateNetworkStatus(result);

    // Listen to connectivity changes
    _subscription = _connectivity.onConnectivityChanged.listen((result) {
      _updateNetworkStatus(result);
    });
  }

  /// Update network status based on connectivity result
  void _updateNetworkStatus(List<ConnectivityResult> results) {
    // If any result indicates connectivity, consider online
    final hasConnection = results.any((result) =>
        result == ConnectivityResult.mobile ||
        result == ConnectivityResult.wifi ||
        result == ConnectivityResult.ethernet ||
        result == ConnectivityResult.vpn);

    final status = hasConnection ? NetworkStatus.online : NetworkStatus.offline;
    _ref.read(networkStatusProvider.notifier).state = status;
  }

  /// Dispose resources
  void dispose() {
    _subscription?.cancel();
  }
}

/// Provider for ConnectivityService
final connectivityServiceProvider = Provider<ConnectivityService>((ref) {
  final service = ConnectivityService(Connectivity(), ref);
  service.initialize();
  ref.onDispose(() => service.dispose());
  return service;
});
