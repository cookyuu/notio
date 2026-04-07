import 'dart:async';
import 'dart:convert';

import 'package:notio_app/features/settings/data/model/settings_model.dart';
import 'package:notio_app/features/settings/domain/entity/settings_entity.dart';
import 'package:notio_app/features/settings/domain/repository/settings_repository.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Implementation of SettingsRepository using SharedPreferences
class SettingsRepositoryImpl implements SettingsRepository {
  static const String _settingsKey = 'app_settings';

  final SharedPreferences _prefs;
  final StreamController<SettingsEntity> _settingsController =
      StreamController<SettingsEntity>.broadcast();

  SettingsRepositoryImpl({
    required SharedPreferences prefs,
  }) : _prefs = prefs;

  @override
  Future<SettingsEntity> loadSettings() async {
    try {
      final jsonString = _prefs.getString(_settingsKey);

      if (jsonString == null) {
        // Return default settings if not found
        return SettingsEntity.defaultSettings();
      }

      final json = jsonDecode(jsonString) as Map<String, dynamic>;
      final model = SettingsModel.fromJson(json);
      return model.toEntity();
    } catch (e) {
      // Return default settings on error
      return SettingsEntity.defaultSettings();
    }
  }

  @override
  Future<void> saveSettings(SettingsEntity settings) async {
    try {
      final model = SettingsModel.fromEntity(settings);
      final jsonString = jsonEncode(model.toJson());
      await _prefs.setString(_settingsKey, jsonString);

      // Notify listeners
      _settingsController.add(settings);
    } catch (e) {
      throw Exception('Failed to save settings: $e');
    }
  }

  @override
  Future<void> updateSetting<T>({
    bool? isDarkMode,
    bool? isPushEnabled,
    dynamic defaultFilter,
  }) async {
    try {
      // Load current settings
      final currentSettings = await loadSettings();

      // Create updated settings
      final updatedSettings = currentSettings.copyWith(
        isDarkMode: isDarkMode,
        isPushEnabled: isPushEnabled,
        defaultFilter: defaultFilter,
      );

      // Save updated settings
      await saveSettings(updatedSettings);
    } catch (e) {
      throw Exception('Failed to update setting: $e');
    }
  }

  @override
  Future<void> resetSettings() async {
    try {
      await _prefs.remove(_settingsKey);

      // Notify listeners with default settings
      _settingsController.add(SettingsEntity.defaultSettings());
    } catch (e) {
      throw Exception('Failed to reset settings: $e');
    }
  }

  @override
  Stream<SettingsEntity> watchSettings() {
    return _settingsController.stream;
  }

  /// Dispose resources
  void dispose() {
    _settingsController.close();
  }
}
