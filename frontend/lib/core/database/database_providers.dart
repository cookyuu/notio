import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/core/database/app_database.dart';

/// Provider for AppDatabase (singleton)
final appDatabaseProvider = Provider<AppDatabase>((ref) {
  final database = AppDatabase();

  // Dispose database when provider is disposed
  ref.onDispose(() {
    database.close();
  });

  return database;
});
