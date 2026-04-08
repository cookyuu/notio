import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/features/settings/data/repository/settings_repository_impl.dart';
import 'package:notio_app/features/settings/domain/entity/settings_entity.dart';
import 'package:notio_app/features/settings/domain/repository/settings_repository.dart';
import 'package:notio_app/features/settings/presentation/providers/settings_notifier.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Provider for SharedPreferences
/// This will be overridden in main.dart with the actual instance
final sharedPreferencesProvider = Provider<SharedPreferences>((ref) {
  throw UnimplementedError('sharedPreferencesProvider must be overridden');
});

/// Provider for settings repository
final settingsRepositoryProvider = Provider<SettingsRepository>((ref) {
  final prefs = ref.watch(sharedPreferencesProvider);
  return SettingsRepositoryImpl(prefs: prefs);
});

/// Provider for settings state
final settingsProvider = StateNotifierProvider<SettingsNotifier, SettingsEntity>((ref) {
  final repository = ref.watch(settingsRepositoryProvider);
  return SettingsNotifier(repository);
});

/// Provider for dark mode state only
final isDarkModeProvider = Provider<bool>((ref) {
  return ref.watch(settingsProvider.select((settings) => settings.isDarkMode));
});

/// Provider for push notification state only
final isPushEnabledProvider = Provider<bool>((ref) {
  return ref.watch(settingsProvider.select((settings) => settings.isPushEnabled));
});
