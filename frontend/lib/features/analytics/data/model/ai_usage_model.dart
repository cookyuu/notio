import 'package:notio_app/features/analytics/domain/entity/ai_usage_entity.dart';

class AiUsageModel {
  final String granularity;
  final String startDate;
  final String endDate;
  final int totalInputTokens;
  final int totalOutputTokens;
  final int totalSessions;
  final String? mostUsedModel;
  final List<AiUsagePeriodPointModel> trend;
  final List<AiUsageModelShareModel> modelDistribution;

  const AiUsageModel({
    required this.granularity,
    required this.startDate,
    required this.endDate,
    required this.totalInputTokens,
    required this.totalOutputTokens,
    required this.totalSessions,
    required this.trend,
    required this.modelDistribution,
    this.mostUsedModel,
  });

  factory AiUsageModel.fromJson(Map<String, dynamic> json) {
    return AiUsageModel(
      granularity: json['granularity'] as String,
      startDate: json['startDate'] as String,
      endDate: json['endDate'] as String,
      totalInputTokens: json['totalInputTokens'] as int,
      totalOutputTokens: json['totalOutputTokens'] as int,
      totalSessions: json['totalSessions'] as int,
      mostUsedModel: json['mostUsedModel'] as String?,
      trend: (json['trend'] as List<dynamic>)
          .map((e) => AiUsagePeriodPointModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      modelDistribution: (json['modelDistribution'] as List<dynamic>)
          .map((e) => AiUsageModelShareModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  AiUsageEntity toEntity() {
    return AiUsageEntity(
      granularity: _parseGranularity(granularity),
      startDate: DateTime.parse(startDate),
      endDate: DateTime.parse(endDate),
      totalInputTokens: totalInputTokens,
      totalOutputTokens: totalOutputTokens,
      totalSessions: totalSessions,
      mostUsedModel: mostUsedModel,
      trend: trend.map((e) => e.toEntity()).toList(),
      modelDistribution: modelDistribution.map((e) => e.toEntity()).toList(),
    );
  }

  static AiUsageGranularity _parseGranularity(String value) {
    switch (value.toUpperCase()) {
      case 'WEEKLY':
        return AiUsageGranularity.weekly;
      case 'MONTHLY':
        return AiUsageGranularity.monthly;
      default:
        return AiUsageGranularity.daily;
    }
  }
}

class AiUsagePeriodPointModel {
  final String label;
  final int inputTokens;
  final int outputTokens;
  final int sessions;

  const AiUsagePeriodPointModel({
    required this.label,
    required this.inputTokens,
    required this.outputTokens,
    required this.sessions,
  });

  factory AiUsagePeriodPointModel.fromJson(Map<String, dynamic> json) {
    return AiUsagePeriodPointModel(
      label: json['label'] as String,
      inputTokens: json['inputTokens'] as int,
      outputTokens: json['outputTokens'] as int,
      sessions: json['sessions'] as int,
    );
  }

  AiUsagePeriodPoint toEntity() {
    return AiUsagePeriodPoint(
      label: label,
      inputTokens: inputTokens,
      outputTokens: outputTokens,
      sessions: sessions,
    );
  }
}

class AiUsageModelShareModel {
  final String model;
  final int totalTokens;
  final int sessions;

  const AiUsageModelShareModel({
    required this.model,
    required this.totalTokens,
    required this.sessions,
  });

  factory AiUsageModelShareModel.fromJson(Map<String, dynamic> json) {
    return AiUsageModelShareModel(
      model: json['model'] as String,
      totalTokens: json['totalTokens'] as int,
      sessions: json['sessions'] as int,
    );
  }

  AiUsageModelShare toEntity() {
    return AiUsageModelShare(
      model: model,
      totalTokens: totalTokens,
      sessions: sessions,
    );
  }
}
