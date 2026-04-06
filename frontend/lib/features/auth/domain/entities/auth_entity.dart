import 'package:drift/drift.dart';

/// Authentication entity for storing user session info
class AuthEntity extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get userId => text()();
  TextColumn get email => text()();
  TextColumn get accessToken => text()();
  TextColumn get refreshToken => text()();
  DateTimeColumn get expiresAt => dateTime()();
  DateTimeColumn get createdAt => dateTime().withDefault(currentDateAndTime)();
  DateTimeColumn get updatedAt => dateTime().withDefault(currentDateAndTime)();
}

/// Auth data class
class AuthData {
  final int id;
  final String userId;
  final String email;
  final String accessToken;
  final String refreshToken;
  final DateTime expiresAt;
  final DateTime createdAt;
  final DateTime updatedAt;

  const AuthData({
    required this.id,
    required this.userId,
    required this.email,
    required this.accessToken,
    required this.refreshToken,
    required this.expiresAt,
    required this.createdAt,
    required this.updatedAt,
  });

  bool get isExpired => DateTime.now().isAfter(expiresAt);
}
