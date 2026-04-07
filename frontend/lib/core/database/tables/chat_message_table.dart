import 'package:drift/drift.dart';

/// Drift table for chat messages
class ChatMessageTable extends Table {
  @override
  String get tableName => 'chat_messages';

  IntColumn get id => integer().autoIncrement()();
  TextColumn get role => text()(); // 'user' or 'assistant'
  TextColumn get content => text()();
  DateTimeColumn get createdAt => dateTime()();
}
