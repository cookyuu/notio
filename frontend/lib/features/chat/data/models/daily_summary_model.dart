/// Data model for daily summary (DTO)
class DailySummaryModel {
  final String summary;
  final String date;
  final int totalMessages;
  final List<String> topics;

  const DailySummaryModel({
    required this.summary,
    required this.date,
    required this.totalMessages,
    required this.topics,
  });

  factory DailySummaryModel.fromJson(Map<String, dynamic> json) {
    return DailySummaryModel(
      summary: json['summary'] as String,
      date: json['date'] as String,
      totalMessages: json['total_messages'] as int,
      topics: (json['topics'] as List<dynamic>).map((e) => e as String).toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'summary': summary,
      'date': date,
      'total_messages': totalMessages,
      'topics': topics,
    };
  }
}
