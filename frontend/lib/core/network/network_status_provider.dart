import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Network connection status
enum NetworkStatus {
  online,
  offline,
  unknown,
}

/// Simple network status provider
/// In production, use connectivity_plus or similar package
final networkStatusProvider = StateProvider<NetworkStatus>((ref) {
  return NetworkStatus.unknown;
});

/// Check if network is online
final isOnlineProvider = Provider<bool>((ref) {
  final status = ref.watch(networkStatusProvider);
  return status == NetworkStatus.online;
});

/// Check if network is offline
final isOfflineProvider = Provider<bool>((ref) {
  final status = ref.watch(networkStatusProvider);
  return status == NetworkStatus.offline;
});
