import 'package:notio_app/core/constants/notification_source.dart';
import 'package:notio_app/features/notification/data/datasource/notification_local_datasource.dart';
import 'package:notio_app/features/notification/data/datasource/notification_remote_datasource.dart';
import 'package:notio_app/features/notification/data/model/notification_model.dart';
import 'package:notio_app/features/notification/domain/entity/notification_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_summary_entity.dart';
import 'package:notio_app/features/notification/domain/entity/notification_detail_entity.dart';
import 'package:notio_app/features/notification/domain/repository/notification_repository.dart';

/// Implementation of NotificationRepository
class NotificationRepositoryImpl implements NotificationRepository {
  final NotificationRemoteDataSource _remoteDataSource;
  final NotificationLocalDataSource _localDataSource;

  NotificationRepositoryImpl({
    required NotificationRemoteDataSource remoteDataSource,
    required NotificationLocalDataSource localDataSource,
  })  : _remoteDataSource = remoteDataSource,
        _localDataSource = localDataSource;

  @override
  Future<List<NotificationSummaryEntity>> fetchNotifications({
    NotificationSource? source,
    int page = 0,
    int size = 20,
  }) async {
    try {
      // Try to fetch from remote
      final summaryModels = await _remoteDataSource.fetchNotifications(
        source: source?.apiValue,
        page: page,
        size: size,
      );

      // Convert to entities
      return summaryModels.map((model) => model.toEntity()).toList();
    } catch (e) {
      // If remote fails, fall back to cached data
      final cachedModels = await _localDataSource.getCachedNotifications(
        source: source?.apiValue,
      );

      // Convert cached full models to summary entities
      return cachedModels.map((model) => model.toSummaryEntity()).toList();
    }
  }

  @override
  Future<NotificationDetailEntity> getNotificationDetail(int id) async {
    final detailModel = await _remoteDataSource.getNotificationDetail(id);
    return detailModel.toEntity();
  }

  @override
  Future<int> getUnreadCount() async {
    try {
      return await _remoteDataSource.getUnreadCount();
    } catch (e) {
      // Fall back to local count
      return await _localDataSource.getUnreadCount();
    }
  }

  @override
  Future<void> markAsRead(int notificationId) async {
    // Update locally first for immediate UI feedback
    await _localDataSource.markAsRead(notificationId);

    // Then sync with remote
    try {
      await _remoteDataSource.markAsRead(notificationId);
    } catch (e) {
      // Remote update failed, but local is already updated
      // TODO: Implement retry mechanism or queue for later sync
    }
  }

  @override
  Future<void> markAllAsRead() async {
    // Update locally first
    await _localDataSource.markAllAsRead();

    // Then sync with remote
    try {
      await _remoteDataSource.markAllAsRead();
    } catch (e) {
      // Remote update failed, but local is already updated
    }
  }

  @override
  Future<NotificationEntity?> getNotificationById(int id) async {
    final model = await _localDataSource.getNotificationById(id);
    return model?.toEntity();
  }

  @override
  Future<void> deleteNotification(int id) async {
    await _localDataSource.deleteNotification(id);

    try {
      await _remoteDataSource.deleteNotification(id);
    } catch (e) {
      // Remote delete failed, but local cache is already updated
    }
  }

  @override
  Future<List<NotificationEntity>> getCachedNotifications({
    NotificationSource? source,
  }) async {
    final models = await _localDataSource.getCachedNotifications(
      source: source?.apiValue,
    );
    return models.map((model) => model.toEntity()).toList();
  }

  @override
  Future<void> cacheNotifications(List<NotificationEntity> notifications) async {
    final models = notifications.map((entity) {
      return NotificationModel.fromEntity(entity);
    }).toList();

    await _localDataSource.cacheNotifications(models);
  }

  @override
  Future<void> clearCache() async {
    await _localDataSource.clearCache();
  }
}
