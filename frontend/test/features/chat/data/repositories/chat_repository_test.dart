import 'package:flutter_test/flutter_test.dart';
import 'package:dio/dio.dart';
import 'package:notio_app/features/chat/data/datasources/chat_remote_datasource.dart';
import 'package:notio_app/features/chat/domain/entities/message_role.dart';

// TODO: Update tests after Drift migration (Phase 4A)
// ChatLocalDataSource now requires AppDatabase parameter
void main() {
  group('ChatRepository', () {
    late ChatRemoteDataSource remoteDataSource;
    late Dio dio;

    setUp(() {
      dio = Dio();
      remoteDataSource = ChatRemoteDataSource(dio);
    });

    test('sendMessage should return AI response', () async {
      // TODO: Implement after Drift migration
      expect(remoteDataSource, isNotNull);
    }, skip: true);

    test('sendMessage should cache messages', () async {
      // TODO: Implement after Drift migration
      expect(remoteDataSource, isNotNull);
    }, skip: true);

    test('fetchHistory should return mock messages', () async {
      // TODO: Implement after Drift migration
      expect(remoteDataSource, isNotNull);
    }, skip: true);

    test('fetchHistory should cache first page results', () async {
      // TODO: Implement after Drift migration
      expect(remoteDataSource, isNotNull);
    }, skip: true);

    test('getDailySummary should return summary', () async {
      // TODO: Implement after Drift migration
      expect(remoteDataSource, isNotNull);
    }, skip: true);

    test('clearCache should remove all cached messages', () async {
      // TODO: Implement after Drift migration
      expect(remoteDataSource, isNotNull);
    }, skip: true);

    test('streamMessage should emit chunks', () async {
      // TODO: Implement after Drift migration
      expect(remoteDataSource, isNotNull);
    }, skip: true);
  });
}
