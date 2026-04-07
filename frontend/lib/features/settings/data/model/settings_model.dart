import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/settings/domain/entity/settings_entity.dart';

/// Data model for app settings (for SharedPreferences storage)
class SettingsModel {
  final bool isDarkMode;
  final bool isPushEnabled;
  final String? defaultFilter;

  const SettingsModel({
    required this.isDarkMode,
    required this.isPushEnabled,
    this.defaultFilter,
  });

  Map<String, dynamic> toJson() {
    return {
      'is_dark_mode': isDarkMode,
      'is_push_enabled': isPushEnabled,
      'default_filter': defaultFilter,
    };
  }

  factory SettingsModel.fromJson(Map<String, dynamic> json) {
    return SettingsModel(
      isDarkMode: json['is_dark_mode'] as bool? ?? true,
      isPushEnabled: json['is_push_enabled'] as bool? ?? true,
      defaultFilter: json['default_filter'] as String?,
    );
  }

  /// Convert to domain entity
  SettingsEntity toEntity() {
    NotificationSource? source;
    if (defaultFilter != null) {
      try {
        source = NotificationSourceExtension.fromApiValue(defaultFilter!);
      } catch (_) {
        source = null;
      }
    }

    return SettingsEntity(
      isDarkMode: isDarkMode,
      isPushEnabled: isPushEnabled,
      defaultFilter: source,
    );
  }

  /// Create from domain entity
  factory SettingsModel.fromEntity(SettingsEntity entity) {
    return SettingsModel(
      isDarkMode: entity.isDarkMode,
      isPushEnabled: entity.isPushEnabled,
      defaultFilter: entity.defaultFilter?.apiValue,
    );
  }
}
