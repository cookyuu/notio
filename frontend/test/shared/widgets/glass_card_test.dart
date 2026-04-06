import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/shared/widgets/glass_card.dart';

void main() {
  group('GlassCard Widget Tests', () {
    testWidgets('renders child correctly', (WidgetTester tester) async {
      const testText = 'Test Child';

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: GlassCard(
              child: Text(testText),
            ),
          ),
        ),
      );

      expect(find.text(testText), findsOneWidget);
    });

    testWidgets('applies custom blur and opacity', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: GlassCard(
              blur: 20.0,
              opacity: 0.2,
              child: Container(),
            ),
          ),
        ),
      );

      expect(find.byType(GlassCard), findsOneWidget);
    });

    testWidgets('applies custom border radius', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: GlassCard(
              borderRadius: 24.0,
              child: Container(),
            ),
          ),
        ),
      );

      expect(find.byType(GlassCard), findsOneWidget);
    });

    testWidgets('applies padding when provided', (WidgetTester tester) async {
      const testPadding = EdgeInsets.all(16);

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: GlassCard(
              padding: testPadding,
              child: Text('Test'),
            ),
          ),
        ),
      );

      expect(find.byType(GlassCard), findsOneWidget);
    });

    testWidgets('can disable border', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: GlassCard(
              border: false,
              child: Container(),
            ),
          ),
        ),
      );

      expect(find.byType(GlassCard), findsOneWidget);
    });
  });

  group('GlassCard Golden Tests', () {
    testWidgets('golden - default parameters', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: Center(
              child: SizedBox(
                width: 200,
                height: 100,
                child: GlassCard(
                  child: Center(
                    child: Text('Glass Card'),
                  ),
                ),
              ),
            ),
          ),
        ),
      );

      await expectLater(
        find.byType(GlassCard),
        matchesGoldenFile('goldens/glass_card_default.png'),
      );
    });

    testWidgets('golden - with padding', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: Center(
              child: SizedBox(
                width: 200,
                height: 100,
                child: GlassCard(
                  padding: EdgeInsets.all(16),
                  child: Text('Padded Glass Card'),
                ),
              ),
            ),
          ),
        ),
      );

      await expectLater(
        find.byType(GlassCard),
        matchesGoldenFile('goldens/glass_card_padded.png'),
      );
    });

    testWidgets('golden - without border', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: Center(
              child: SizedBox(
                width: 200,
                height: 100,
                child: GlassCard(
                  border: false,
                  child: Center(
                    child: Text('No Border'),
                  ),
                ),
              ),
            ),
          ),
        ),
      );

      await expectLater(
        find.byType(GlassCard),
        matchesGoldenFile('goldens/glass_card_no_border.png'),
      );
    });
  });
}
