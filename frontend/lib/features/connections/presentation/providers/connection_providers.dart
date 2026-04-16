import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:notio_app/core/network/dio_client.dart';
import 'package:notio_app/shared/constant/api_constants.dart';
import '../../data/datasource/connection_remote_datasource.dart';
import '../../data/repository/connection_repository_impl.dart';
import '../../domain/repository/connection_repository.dart';
import '../../domain/entity/connection_entity.dart';
import '../../domain/entity/connection_provider.dart';
import '../../domain/entity/connection_auth_type.dart';
import '../../domain/entity/connection_status.dart';
import '../../data/model/connection_create_response.dart';
import '../../data/model/connection_test_response.dart';
import '../../data/model/connection_refresh_response.dart';
import '../../data/model/connection_rotate_key_response.dart';

part 'connection_providers.g.dart';

/// Dio Provider (shared across features)
final dioProvider = Provider<Dio>((ref) {
  return DioClient.create(
    baseUrl: ApiConstants.baseUrl,
    enableLogging: true,
  );
});

/// Provider for connection remote data source
final connectionRemoteDataSourceProvider =
    Provider<ConnectionRemoteDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return ConnectionRemoteDataSource(dio);
});

/// Provider for connection repository
///
/// IMPORTANT: This repository does NOT cache API keys or sensitive credentials
/// API keys are only returned in create/rotate responses and should be displayed once
final connectionRepositoryProvider = Provider<ConnectionRepository>((ref) {
  final remoteDataSource = ref.watch(connectionRemoteDataSourceProvider);
  return ConnectionRepositoryImpl(remoteDataSource: remoteDataSource);
});

// ============================================================================
// Phase 2: State Management Providers
// ============================================================================

/// Connections list provider (AsyncNotifier pattern)
///
/// Manages the list of connections with loading, error, and success states
/// Supports refresh and filter operations
@riverpod
class Connections extends _$Connections {
  @override
  Future<List<ConnectionEntity>> build() async {
    final repository = ref.watch(connectionRepositoryProvider);
    return repository.fetchConnections();
  }

  /// Refresh connections list
  Future<void> refresh() async {
    state = const AsyncValue.loading();
    state = await AsyncValue.guard(() async {
      final repository = ref.read(connectionRepositoryProvider);
      return repository.fetchConnections();
    });
  }

  /// Get filtered connections based on current filters
  List<ConnectionEntity> getFiltered() {
    return state.when(
      data: (connections) {
        var filtered = connections;

        // Apply provider filter
        final providerFilter = ref.read(connectionProviderFilterProvider);
        if (providerFilter != null) {
          filtered = filtered
              .where((c) => c.provider == providerFilter)
              .toList();
        }

        // Apply status filter
        final statusFilter = ref.read(connectionStatusFilterProvider);
        if (statusFilter != null) {
          filtered = filtered
              .where((c) => c.status == statusFilter)
              .toList();
        }

        // Apply auth type filter
        final authTypeFilter = ref.read(connectionAuthTypeFilterProvider);
        if (authTypeFilter != null) {
          filtered = filtered
              .where((c) => c.authType == authTypeFilter)
              .toList();
        }

        return filtered;
      },
      loading: () => [],
      error: (_, __) => [],
    );
  }
}

/// Connection detail provider (family pattern for individual connection)
@riverpod
Future<ConnectionEntity> connectionDetail(
  ConnectionDetailRef ref,
  int id,
) async {
  final repository = ref.watch(connectionRepositoryProvider);
  return repository.fetchConnectionById(id);
}

// ============================================================================
// Filter Providers
// ============================================================================

/// Provider filter (null = show all)
final connectionProviderFilterProvider =
    StateProvider<ConnectionProvider?>((ref) => null);

/// Status filter (null = show all)
final connectionStatusFilterProvider =
    StateProvider<ConnectionStatus?>((ref) => null);

/// Auth type filter (null = show all)
final connectionAuthTypeFilterProvider =
    StateProvider<ConnectionAuthType?>((ref) => null);

// ============================================================================
// One-time API Key State
// ============================================================================

/// One-time API key state
///
/// IMPORTANT: This state holds the API key ONLY temporarily for display
/// The key is discarded when the dialog is closed
/// Never store this key in local storage, shared preferences, or database
class OneTimeApiKeyState {
  final String? apiKey;
  final bool isVisible;

  const OneTimeApiKeyState({
    this.apiKey,
    this.isVisible = false,
  });

  OneTimeApiKeyState copyWith({
    String? apiKey,
    bool? isVisible,
  }) {
    return OneTimeApiKeyState(
      apiKey: apiKey ?? this.apiKey,
      isVisible: isVisible ?? this.isVisible,
    );
  }
}

/// One-time API key provider
///
/// Manages the temporary display of API keys from create/rotate operations
final oneTimeApiKeyProvider =
    StateNotifierProvider<OneTimeApiKeyNotifier, OneTimeApiKeyState>((ref) {
  return OneTimeApiKeyNotifier();
});

class OneTimeApiKeyNotifier extends StateNotifier<OneTimeApiKeyState> {
  OneTimeApiKeyNotifier() : super(const OneTimeApiKeyState());

  /// Show API key (from create or rotate response)
  void showApiKey(String apiKey) {
    state = OneTimeApiKeyState(apiKey: apiKey, isVisible: true);
  }

  /// Discard API key (when dialog is closed)
  /// IMPORTANT: This ensures the key cannot be retrieved again
  void discardApiKey() {
    state = const OneTimeApiKeyState(apiKey: null, isVisible: false);
  }
}

// ============================================================================
// OAuth State
// ============================================================================

/// OAuth state
class OAuthState {
  final bool isInProgress;
  final String? authorizationUrl;
  final String? errorMessage;

  const OAuthState({
    this.isInProgress = false,
    this.authorizationUrl,
    this.errorMessage,
  });

  OAuthState copyWith({
    bool? isInProgress,
    String? authorizationUrl,
    String? errorMessage,
  }) {
    return OAuthState(
      isInProgress: isInProgress ?? this.isInProgress,
      authorizationUrl: authorizationUrl ?? this.authorizationUrl,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }
}

/// OAuth state provider
final oauthStateProvider =
    StateNotifierProvider<OAuthStateNotifier, OAuthState>((ref) {
  return OAuthStateNotifier(ref);
});

class OAuthStateNotifier extends StateNotifier<OAuthState> {
  final Ref ref;

  OAuthStateNotifier(this.ref) : super(const OAuthState());

  /// Start OAuth flow
  Future<void> startOAuth({
    required ConnectionProvider provider,
    required String displayName,
    String? redirectUri,
  }) async {
    state = state.copyWith(isInProgress: true, errorMessage: null);

    try {
      final repository = ref.read(connectionRepositoryProvider);
      final response = await repository.requestOAuthUrl(
        provider: provider,
        displayName: displayName,
        redirectUri: redirectUri,
      );

      state = state.copyWith(
        isInProgress: false,
        authorizationUrl: response.authorizationUrl,
      );
    } catch (e) {
      state = state.copyWith(
        isInProgress: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// Reset OAuth state
  void reset() {
    state = const OAuthState();
  }
}

// ============================================================================
// Connection Actions Notifier
// ============================================================================

/// Connection action state
class ConnectionActionState {
  final bool isCreating;
  final bool isDeleting;
  final bool isTesting;
  final bool isRefreshing;
  final bool isRotating;
  final String? successMessage;
  final String? errorMessage;

  const ConnectionActionState({
    this.isCreating = false,
    this.isDeleting = false,
    this.isTesting = false,
    this.isRefreshing = false,
    this.isRotating = false,
    this.successMessage,
    this.errorMessage,
  });

  ConnectionActionState copyWith({
    bool? isCreating,
    bool? isDeleting,
    bool? isTesting,
    bool? isRefreshing,
    bool? isRotating,
    String? successMessage,
    String? errorMessage,
  }) {
    return ConnectionActionState(
      isCreating: isCreating ?? this.isCreating,
      isDeleting: isDeleting ?? this.isDeleting,
      isTesting: isTesting ?? this.isTesting,
      isRefreshing: isRefreshing ?? this.isRefreshing,
      isRotating: isRotating ?? this.isRotating,
      successMessage: successMessage,
      errorMessage: errorMessage,
    );
  }

  bool get isAnyLoading =>
      isCreating || isDeleting || isTesting || isRefreshing || isRotating;
}

/// Connection actions provider
final connectionActionsProvider = StateNotifierProvider<
    ConnectionActionsNotifier, ConnectionActionState>((ref) {
  return ConnectionActionsNotifier(ref);
});

class ConnectionActionsNotifier extends StateNotifier<ConnectionActionState> {
  final Ref ref;

  ConnectionActionsNotifier(this.ref) : super(const ConnectionActionState());

  /// Create new connection
  Future<void> createConnection({
    required ConnectionProvider provider,
    required ConnectionAuthType authType,
    required String displayName,
  }) async {
    state = state.copyWith(isCreating: true, errorMessage: null);

    try {
      final repository = ref.read(connectionRepositoryProvider);
      final response = await repository.createConnection(
        provider: provider,
        authType: authType,
        displayName: displayName,
      );

      // Show one-time API key if present
      if (response.apiKey != null) {
        ref.read(oneTimeApiKeyProvider.notifier).showApiKey(response.apiKey!);
      }

      // Refresh connections list
      await ref.read(connectionsProvider.notifier).refresh();

      state = state.copyWith(
        isCreating: false,
        successMessage: 'Connection created successfully',
      );
    } catch (e) {
      state = state.copyWith(
        isCreating: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// Delete connection
  Future<void> deleteConnection(int id) async {
    state = state.copyWith(isDeleting: true, errorMessage: null);

    try {
      final repository = ref.read(connectionRepositoryProvider);
      await repository.deleteConnection(id);

      // Refresh connections list
      await ref.read(connectionsProvider.notifier).refresh();

      state = state.copyWith(
        isDeleting: false,
        successMessage: 'Connection deleted successfully',
      );
    } catch (e) {
      state = state.copyWith(
        isDeleting: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// Test connection
  Future<void> testConnection(int id) async {
    state = state.copyWith(isTesting: true, errorMessage: null);

    try {
      final repository = ref.read(connectionRepositoryProvider);
      final response = await repository.testConnection(id);

      state = state.copyWith(
        isTesting: false,
        successMessage: response.success
            ? response.message ?? 'Connection test successful'
            : null,
        errorMessage: !response.success ? response.message : null,
      );
    } catch (e) {
      state = state.copyWith(
        isTesting: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// Refresh OAuth token
  Future<void> refreshConnection(int id) async {
    state = state.copyWith(isRefreshing: true, errorMessage: null);

    try {
      final repository = ref.read(connectionRepositoryProvider);
      final response = await repository.refreshConnection(id);

      // Refresh connections list to reflect updated status
      await ref.read(connectionsProvider.notifier).refresh();

      state = state.copyWith(
        isRefreshing: false,
        successMessage: response.success
            ? response.message ?? 'Token refreshed successfully'
            : null,
        errorMessage: !response.success ? response.message : null,
      );
    } catch (e) {
      state = state.copyWith(
        isRefreshing: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// Rotate API key
  Future<void> rotateKey(int id) async {
    state = state.copyWith(isRotating: true, errorMessage: null);

    try {
      final repository = ref.read(connectionRepositoryProvider);
      final response = await repository.rotateKey(id);

      // Show one-time API key
      if (response.apiKey != null) {
        ref.read(oneTimeApiKeyProvider.notifier).showApiKey(response.apiKey!);
      }

      // Refresh connections list to reflect updated key preview
      await ref.read(connectionsProvider.notifier).refresh();

      state = state.copyWith(
        isRotating: false,
        successMessage: response.success
            ? response.message ?? 'API key rotated successfully'
            : null,
        errorMessage: !response.success ? response.message : null,
      );
    } catch (e) {
      state = state.copyWith(
        isRotating: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// Clear messages
  void clearMessages() {
    state = state.copyWith(
      successMessage: null,
      errorMessage: null,
    );
  }
}
