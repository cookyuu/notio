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
      granularity: (json['granularity'] as String?) ?? 'DAILY',
      startDate: (json['start_date'] as String?) ?? '',
      endDate: (json['end_date'] as String?) ?? '',
      totalInputTokens: (json['total_input_tokens'] as int?) ?? 0,
      totalOutputTokens: (json['total_output_tokens'] as int?) ?? 0,
      totalSessions: (json['total_sessions'] as int?) ?? 0,
      mostUsedModel: json['most_used_model'] as String?,
      trend: ((json['trend'] as List<dynamic>?) ?? [])
          .map((e) => AiUsagePeriodPointModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      modelDistribution: ((json['model_distribution'] as List<dynamic>?) ?? [])
          .map((e) => AiUsageModelShareModel.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  AiUsageEntity toEntity() {
    return AiUsageEntity(
      granularity: _parseGranularity(granularity),
      startDate: startDate.isNotEmpty ? DateTime.parse(startDate) : DateTime.now(),
      endDate: endDate.isNotEmpty ? DateTime.parse(endDate) : DateTime.now(),
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
      label: (json['label'] as String?) ?? '',
      inputTokens: (json['input_tokens'] as int?) ?? 0,
      outputTokens: (json['output_tokens'] as int?) ?? 0,
      sessions: (json['sessions'] as int?) ?? 0,
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
      model: (json['model'] as String?) ?? '',
      totalTokens: (json['total_tokens'] as int?) ?? 0,
      sessions: (json['sessions'] as int?) ?? 0,
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
