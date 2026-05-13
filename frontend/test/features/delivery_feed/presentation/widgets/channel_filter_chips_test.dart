import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/delivery_feed/domain/entity/channel_type_enum.dart';
import 'package:notio_app/features/delivery_feed/presentation/widgets/channel_filter_chips.dart';

void main() {
  group('ChannelFilterChips', () {
    testWidgets('renders All, Slack, Telegram, Discord chips', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ChannelFilterChips(selected: null, onSelected: (_) {}),
          ),
        ),
      );

      expect(find.widgetWithText(FilterChip, 'All'), findsOneWidget);
      expect(find.widgetWithText(FilterChip, 'Slack'), findsOneWidget);
      expect(find.widgetWithText(FilterChip, 'Telegram'), findsOneWidget);
      expect(find.widgetWithText(FilterChip, 'Discord'), findsOneWidget);
    });

    group('선택 상태', () {
      testWidgets('All chip is selected when filter is null', (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChannelFilterChips(selected: null, onSelected: (_) {}),
            ),
          ),
        );

        final allChip = tester.widget<FilterChip>(
          find.widgetWithText(FilterChip, 'All'),
        );
        expect(allChip.selected, isTrue);

        for (final label in ['Slack', 'Telegram', 'Discord']) {
          final chip = tester.widget<FilterChip>(
            find.widgetWithText(FilterChip, label),
          );
          expect(chip.selected, isFalse, reason: '$label should not be selected');
        }
      });

      testWidgets('Slack chip is selected when filter is slack', (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChannelFilterChips(
                selected: ChannelTypeEnum.slack,
                onSelected: (_) {},
              ),
            ),
          ),
        );

        final slackChip = tester.widget<FilterChip>(
          find.widgetWithText(FilterChip, 'Slack'),
        );
        expect(slackChip.selected, isTrue);

        final allChip = tester.widget<FilterChip>(
          find.widgetWithText(FilterChip, 'All'),
        );
        expect(allChip.selected, isFalse);
      });

      testWidgets('Telegram chip is selected when filter is telegram',
          (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChannelFilterChips(
                selected: ChannelTypeEnum.telegram,
                onSelected: (_) {},
              ),
            ),
          ),
        );

        final chip = tester.widget<FilterChip>(
          find.widgetWithText(FilterChip, 'Telegram'),
        );
        expect(chip.selected, isTrue);
      });

      testWidgets('Discord chip is selected when filter is discord',
          (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChannelFilterChips(
                selected: ChannelTypeEnum.discord,
                onSelected: (_) {},
              ),
            ),
          ),
        );

        final chip = tester.widget<FilterChip>(
          find.widgetWithText(FilterChip, 'Discord'),
        );
        expect(chip.selected, isTrue);
      });
    });

    group('필터 선택 콜백', () {
      testWidgets('tapping Slack chip calls onSelected with slack',
          (tester) async {
        ChannelTypeEnum? selected;
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChannelFilterChips(
                selected: null,
                onSelected: (v) => selected = v,
              ),
            ),
          ),
        );

        await tester.tap(find.widgetWithText(FilterChip, 'Slack'));
        await tester.pump();
        expect(selected, ChannelTypeEnum.slack);
      });

      testWidgets('tapping Telegram chip calls onSelected with telegram',
          (tester) async {
        ChannelTypeEnum? selected;
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChannelFilterChips(
                selected: null,
                onSelected: (v) => selected = v,
              ),
            ),
          ),
        );

        await tester.tap(find.widgetWithText(FilterChip, 'Telegram'));
        await tester.pump();
        expect(selected, ChannelTypeEnum.telegram);
      });

      testWidgets('tapping Discord chip calls onSelected with discord',
          (tester) async {
        ChannelTypeEnum? selected;
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChannelFilterChips(
                selected: null,
                onSelected: (v) => selected = v,
              ),
            ),
          ),
        );

        await tester.tap(find.widgetWithText(FilterChip, 'Discord'));
        await tester.pump();
        expect(selected, ChannelTypeEnum.discord);
      });

      testWidgets('tapping All chip calls onSelected with null', (tester) async {
        ChannelTypeEnum? selected = ChannelTypeEnum.slack;
        await tester.pumpWidget(
          MaterialApp(
            home: StatefulBuilder(
              builder: (context, setState) => Scaffold(
                body: ChannelFilterChips(
                  selected: selected,
                  onSelected: (v) => setState(() => selected = v),
                ),
              ),
            ),
          ),
        );

        await tester.tap(find.widgetWithText(FilterChip, 'All'));
        await tester.pump();
        expect(selected, isNull);
      });
    });
  });
}
