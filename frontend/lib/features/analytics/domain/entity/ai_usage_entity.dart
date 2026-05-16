enum AiUsageGranularity { daily, weekly, monthly }

class AiUsagePeriodPoint {
  final String label;
  final int inputTokens;
  final int outputTokens;
  final int sessions;

  const AiUsagePeriodPoint({
    required this.label,
    required this.inputTokens,
    required this.outputTokens,
    required this.sessions,
  });
}

class AiUsageModelShare {
  final String model;
  final int totalTokens;
  final int sessions;

  const AiUsageModelShare({
    required this.model,
    required this.totalTokens,
    required this.sessions,
  });
}

class AiUsageEntity {
  final AiUsageGranularity granularity;
  final DateTime startDate;
  final DateTime endDate;
  final int totalInputTokens;
  final int totalOutputTokens;
  final int totalSessions;
  final String? mostUsedModel;
  final List<AiUsagePeriodPoint> trend;
  final List<AiUsageModelShare> modelDistribution;

  const AiUsageEntity({
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

  int get totalTokens => totalInputTokens + totalOutputTokens;

  double get avgTokensPerSession =>
      totalSessions == 0 ? 0.0 : totalTokens / totalSessions;
}
