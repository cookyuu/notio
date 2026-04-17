import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/core/database/app_database.dart';
import 'package:notio_app/features/notification/data/datasource/notification_local_datasource.dart';
import 'package:notio_app/features/notification/data/datasource/notification_remote_datasource.dart';
import 'package:notio_app/features/notification/data/model/notification_detail_model.dart';
import 'package:notio_app/features/notification/data/model/notification_model.dart';
import 'package:notio_app/features/notification/data/model/notification_summary_model.dart';
import 'package:notio_app/features/notification/data/repository/notification_repository_impl.dart';
import 'package:notio_app/features/notification/domain/entity/notification_priority.dart';

class _FakeRemoteDataSource extends NotificationRemoteDataSource {
  _FakeRemoteDataSource(this.results) : super(Dio());

  final List<NotificationSummaryModel> results;
  NotificationDetailModel? detailResult;

  @override
  Future<List<NotificationSummaryModel>> fetchNotifications({
    String? source,
    int page = 0,
    int size = 20,
  }) async {
    return results;
  }

  @override
  Future<NotificationDetailModel> getNotificationDetail(int id) async {
    if (detailResult == null) {
      throw Exception('Missing detail');
    }
    return detailResult!;
  }
}

class _ThrowingLocalDataSource extends NotificationLocalDataSource {
  _ThrowingLocalDataSource(super.database, this.cachedModels)
      : _database = database;

  final AppDatabase _database;
  final List<NotificationModel> cachedModels;

  @override
  Future<void> cacheNotificationSummaries(
    List<NotificationSummaryModel> notifications,
  ) async {
    throw Exception('cache failed');
  }

  @override
  Future<void> markAsRead(int notificationId) async {
    throw Exception('mark as read failed');
  }

  @override
  Future<List<NotificationModel>> getCachedNotifications({
    String? source,
  }) async {
    return cachedModels;
  }

  Future<void> dispose() => _database.close();
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const pathProviderChannel = MethodChannel('plugins.flutter.io/path_provider');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(pathProviderChannel, (call) async {
      if (call.method == 'getApplicationDocumentsDirectory') {
        return Directory.systemTemp.path;
      }
      return null;
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(pathProviderChannel, null);
  });

  group('NotificationRepository', () {
    test('fetchNotifications returns remote results even when cache write fails',
        () async {
      final database = AppDatabase();
      final localDataSource = _ThrowingLocalDataSource(database, const []);
      addTearDown(localDataSource.dispose);

      final repository = NotificationRepositoryImpl(
        remoteDataSource: _FakeRemoteDataSource(
          const [
            NotificationSummaryModel(
              id: 54,
              source: 'GMAIL',
              title: 'Gmail 메일 알림 15',
              bodyPreview: '읽지 않은 메일이 도착했습니다. sample=15',
              priority: 'HIGH',
              isRead: true,
              createdAt: '2026-04-14T01:50:33.944258Z',
            ),
          ],
        ),
        localDataSource: localDataSource,
      );

      final notifications = await repository.fetchNotifications(
        source: NotificationSource.gmail,
        page: 0,
        size: 20,
      );

      expect(notifications, hasLength(1));
      expect(notifications.single.id, 54);
      expect(notifications.single.source, NotificationSource.gmail);
      expect(notifications.single.priority, NotificationPriority.high);
      expect(
        notifications.single.bodyPreview,
        '읽지 않은 메일이 도착했습니다. sample=15',
      );
    });

    test('getNotificationDetail returns remote detail even when local sync fails',
        () async {
      final database = AppDatabase();
      final localDataSource = _ThrowingLocalDataSource(database, const []);
      addTearDown(localDataSource.dispose);

      final remoteDataSource = _FakeRemoteDataSource(const [])
        ..detailResult = const NotificationDetailModel(
          id: 23,
          source: 'GITHUB',
          title: 'GitHub PR/Issue 알림 2',
          body: '리뷰 요청 또는 이슈 업데이트가 발생했습니다. sample=2',
          priority: 'MEDIUM',
          isRead: true,
          createdAt: '2026-04-14T01:32:24.877032Z',
          updatedAt: '2026-04-17T08:36:40.430272Z',
          externalId: 'github-20260414155859934-2',
          externalUrl: 'https://github.com/notio/notio/issues/2',
          metadata: {
            'repo': 'notio/notio',
            'sample': true,
            'eventType': 'issue',
          },
        );

      final repository = NotificationRepositoryImpl(
        remoteDataSource: remoteDataSource,
        localDataSource: localDataSource,
      );

      final detail = await repository.getNotificationDetail(23);

      expect(detail.id, 23);
      expect(detail.source, NotificationSource.github);
      expect(detail.isRead, isTrue);
      expect(detail.externalUrl, 'https://github.com/notio/notio/issues/2');
      expect(detail.metadata?['eventType'], 'issue');
    });
  });
}
