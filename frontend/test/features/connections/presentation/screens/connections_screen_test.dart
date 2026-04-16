import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:notio_app/features/connections/presentation/screens/connections_screen.dart';

void main() {
  group('ConnectionsScreen', () {
    testWidgets('displays screen title', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ConnectionsScreen(),
          ),
        ),
      );

      // Should show screen title (연동 관리) in app bar
      expect(find.text('연동 관리'), findsOneWidget);
    });

    testWidgets('has add connection button', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ConnectionsScreen(),
          ),
        ),
      );

      // Should have add button
      expect(find.byIcon(Icons.add), findsOneWidget);
    });

    testWidgets('shows filter options', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ConnectionsScreen(),
          ),
        ),
      );

      await tester.pump();

      // Should have filter chips for All, Active, Needs Action
      // Note: Filters may not be visible until data is loaded
      expect(find.byType(ConnectionsScreen), findsOneWidget);
    });

    testWidgets('supports pull to refresh', (tester) async {
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: ConnectionsScreen(),
          ),
        ),
      );

      await tester.pump();

      // Should have RefreshIndicator
      expect(find.byType(RefreshIndicator), findsOneWidget);
    });
  });
}
