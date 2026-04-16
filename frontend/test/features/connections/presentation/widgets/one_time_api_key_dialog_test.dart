import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/features/connections/presentation/screens/one_time_api_key_dialog.dart';

void main() {
  group('OneTimeApiKeyDialog', () {
    testWidgets('displays API key', (tester) async {
      const testApiKey = 'sk-test-api-key-1234567890';

      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: Scaffold(
              body: OneTimeApiKeyDialog(apiKey: testApiKey),
            ),
          ),
        ),
      );

      expect(find.text(testApiKey), findsOneWidget);
    });

    testWidgets('displays warning message', (tester) async {
      const testApiKey = 'sk-test-key';

      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: Scaffold(
              body: OneTimeApiKeyDialog(apiKey: testApiKey),
            ),
          ),
        ),
      );

      expect(
        find.text('This is the only time you will see this key. Please copy it now.'),
        findsOneWidget,
      );
    });

    testWidgets('shows copy button', (tester) async {
      const testApiKey = 'sk-test-key';

      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: Scaffold(
              body: OneTimeApiKeyDialog(apiKey: testApiKey),
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.copy), findsOneWidget);
    });

    testWidgets('copy button changes to check icon when clicked', (tester) async {
      const testApiKey = 'sk-test-key';

      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: Scaffold(
              body: OneTimeApiKeyDialog(apiKey: testApiKey),
            ),
          ),
        ),
      );

      // Initially shows copy icon
      expect(find.byIcon(Icons.copy), findsOneWidget);
      expect(find.byIcon(Icons.check), findsNothing);

      // Tap copy button
      await tester.tap(find.byIcon(Icons.copy));
      await tester.pump();

      // Now shows check icon
      expect(find.byIcon(Icons.copy), findsNothing);
      expect(find.byIcon(Icons.check), findsOneWidget);
    });

    testWidgets('displays usage example section', (tester) async {
      const testApiKey = 'sk-test-key';

      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: Scaffold(
              body: OneTimeApiKeyDialog(apiKey: testApiKey),
            ),
          ),
        ),
      );

      expect(find.text('Usage Example (Claude Code)'), findsOneWidget);
      expect(find.textContaining('NOTIO_WEBHOOK_API_KEY='), findsOneWidget);
    });

    testWidgets('has close button', (tester) async {
      const testApiKey = 'sk-test-key';

      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: Scaffold(
              body: OneTimeApiKeyDialog(apiKey: testApiKey),
            ),
          ),
        ),
      );

      expect(find.byIcon(Icons.close), findsOneWidget);
    });
  });
}
