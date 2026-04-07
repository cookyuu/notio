import 'package:notio_app/core/constants/notification_source.dart';

/// Domain entity for app settings
class SettingsEntity {
  final bool isDarkMode;
  final bool isPushEnabled;
  final NotificationSource? defaultFilter;

  const SettingsEntity({
    required this.isDarkMode,
    required this.isPushEnabled,
    this.defaultFilter,
  });

  /// Default settings
  factory SettingsEntity.defaultSettings() {
    return const SettingsEntity(
      isDarkMode: true, // Default to dark mode
      isPushEnabled: true, // Default to push enabled
      defaultFilter: null, // Default to show all
    );
  }

  /// Copy with
  SettingsEntity copyWith({
    bool? isDarkMode,
    bool? isPushEnabled,
    NotificationSource? defaultFilter,
    bool clearDefaultFilter = false,
  }) {
    return SettingsEntity(
      isDarkMode: isDarkMode ?? this.isDarkMode,
      isPushEnabled: isPushEnabled ?? this.isPushEnabled,
      defaultFilter: clearDefaultFilter
          ? null
          : (defaultFilter ?? this.defaultFilter),
    );
  }

  @override
  String toString() {
    return 'SettingsEntity(isDarkMode: $isDarkMode, isPushEnabled: $isPushEnabled, defaultFilter: $defaultFilter)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is SettingsEntity &&
        other.isDarkMode == isDarkMode &&
        other.isPushEnabled == isPushEnabled &&
        other.defaultFilter == defaultFilter;
  }

  @override
  int get hashCode {
    return isDarkMode.hashCode ^
        isPushEnabled.hashCode ^
        defaultFilter.hashCode;
  }
}
