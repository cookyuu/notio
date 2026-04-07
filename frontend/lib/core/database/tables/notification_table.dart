import 'package:drift/drift.dart';

/// Drift table for notifications
class NotificationTable extends Table {
  @override
  String get tableName => 'notifications';

  IntColumn get id => integer().autoIncrement()();
  TextColumn get source => text()();
  TextColumn get title => text()();
  TextColumn get body => text()();
  TextColumn get priority => text()();
  BoolColumn get isRead => boolean().withDefault(const Constant(false))();
  DateTimeColumn get createdAt => dateTime()();
  TextColumn get externalId => text().nullable()();
  TextColumn get externalUrl => text().nullable()();
  TextColumn get metadata => text().nullable()(); // JSON string
}
