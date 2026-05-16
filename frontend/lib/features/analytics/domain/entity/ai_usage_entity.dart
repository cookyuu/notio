enum AiUsageGranularity { daily, weekly, monthly }

class AiUsageFilter {
  final AiUsageGranularity granularity;
  final DateTime startDate;
  final DateTime endDate;

  const AiUsageFilter({
    required this.granularity,
    required this.startDate,
    required this.endDate,
  });

  static AiUsageFilter defaultFor(AiUsageGranularity g) {
    final now = DateTime.now();
    return switch (g) {
      AiUsageGranularity.daily => AiUsageFilter(
          granularity: g,
          startDate: now.subtract(const Duration(days: 6)),
          endDate: now,
        ),
      AiUsageGranularity.weekly => AiUsageFilter(
          granularity: g,
          startDate: now.subtract(const Duration(days: 55)),
          endDate: now,
        ),
      AiUsageGranularity.monthly => AiUsageFilter(
          granularity: g,
          startDate: DateTime(now.year - 1, now.month, now.day),
          endDate: now,
        ),
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is AiUsageFilter &&
          granularity == other.granularity &&
          startDate == other.startDate &&
          endDate == other.endDate;

  @override
  int get hashCode => Object.hash(granularity, startDate, endDate);
}

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
