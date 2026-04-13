// ignore_for_file: deprecated_member_use

import 'package:drift/drift.dart';
import 'package:drift/web.dart';

DatabaseConnection connect() {
  return DatabaseConnection.delayed(Future(() async {
    final db = WebDatabase('notio_db');
    return DatabaseConnection(db);
  }));
}
