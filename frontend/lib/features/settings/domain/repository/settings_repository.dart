import 'package:notio_app/features/settings/domain/entity/settings_entity.dart';

/// Abstract repository for app settings
abstract class SettingsRepository {
  /// Load settings from persistent storage
  Future<SettingsEntity> loadSettings();

  /// Save settings to persistent storage
  Future<void> saveSettings(SettingsEntity settings);

  /// Update a specific setting
  Future<void> updateSetting<T>({
    bool? isDarkMode,
    bool? isPushEnabled,
    dynamic defaultFilter,
  });

  /// Reset settings to default
  Future<void> resetSettings();

  /// Watch settings changes
  Stream<SettingsEntity> watchSettings();
}
