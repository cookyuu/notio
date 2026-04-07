import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/settings/domain/entity/settings_entity.dart';
import 'package:notio_app/features/settings/domain/repository/settings_repository.dart';

/// Settings Notifier
class SettingsNotifier extends StateNotifier<SettingsEntity> {
  final SettingsRepository _repository;

  SettingsNotifier(this._repository) : super(SettingsEntity.defaultSettings()) {
    _loadSettings();
  }

  /// Load settings from repository
  Future<void> _loadSettings() async {
    try {
      final settings = await _repository.loadSettings();
      state = settings;
    } catch (e) {
      // Use default settings on error
      state = SettingsEntity.defaultSettings();
    }
  }

  /// Toggle dark mode
  Future<void> toggleDarkMode() async {
    try {
      final updatedSettings = state.copyWith(isDarkMode: !state.isDarkMode);
      state = updatedSettings;
      await _repository.saveSettings(updatedSettings);
    } catch (e) {
      // Revert on error
      _loadSettings();
    }
  }

  /// Toggle push notification
  Future<void> togglePushNotification() async {
    try {
      final updatedSettings = state.copyWith(isPushEnabled: !state.isPushEnabled);
      state = updatedSettings;
      await _repository.saveSettings(updatedSettings);
    } catch (e) {
      // Revert on error
      _loadSettings();
    }
  }

  /// Set default filter
  Future<void> setDefaultFilter(NotificationSource? filter) async {
    try {
      final updatedSettings = state.copyWith(
        defaultFilter: filter,
        clearDefaultFilter: filter == null,
      );
      state = updatedSettings;
      await _repository.saveSettings(updatedSettings);
    } catch (e) {
      // Revert on error
      _loadSettings();
    }
  }

  /// Reset to default settings
  Future<void> resetSettings() async {
    try {
      await _repository.resetSettings();
      state = SettingsEntity.defaultSettings();
    } catch (e) {
      // Do nothing on error
    }
  }

  /// Reload settings
  Future<void> reload() async {
    await _loadSettings();
  }
}
