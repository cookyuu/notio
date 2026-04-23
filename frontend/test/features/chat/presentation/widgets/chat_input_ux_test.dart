// Phase 3 Chat 입력 UX 점검 테스트
//
// 검증 항목:
// - 자연어 기간 표현이 UI에서 잘리지 않고 전송된다 (maxLines: null 보장)
// - 전송 버튼으로 텍스트가 전송된다
// - 기간 표현 포함 메시지가 chat bubble에 원문 그대로 표시된다
// - 프론트에서 content를 가공하지 않고 그대로 onSend에 전달한다
// - 기간 필터 전용 UI(DatePicker, RangePicker 등)가 존재하지 않는다

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/chat/domain/entities/chat_message_entity.dart';
import 'package:notio_app/features/chat/domain/entities/message_role.dart';
import 'package:notio_app/features/chat/presentation/widgets/chat_input_field.dart';
import 'package:notio_app/features/chat/presentation/widgets/chat_message_bubble.dart';

void main() {
  group('Phase 3 - Chat 입력 UX 점검', () {
    // ---------------------------------------------------------------
    // 1 & 2. ChatInputField: 자연어 기간 표현이 잘리지 않고 전송된다
    // ---------------------------------------------------------------

    group('ChatInputField', () {
      testWidgets(
          '최근 5시간 내의 알림 내역을 요약해줘 입력이 잘리지 않고 전송된다',
          (tester) async {
        const query = '최근 5시간 내의 알림 내역을 요약해줘';
        String? sentContent;

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChatInputField(
                onSend: (content) => sentContent = content,
              ),
            ),
          ),
        );

        await tester.enterText(find.byType(TextField), query);
        await tester.pump();

        // 전송 버튼 탭
        await tester.tap(find.byIcon(Icons.send_rounded));
        await tester.pump();

        expect(sentContent, equals(query));
      });

      testWidgets(
          '지난 30분 동안 중요한 알림 알려줘 입력이 전송 버튼으로 전송된다',
          (tester) async {
        const query = '지난 30분 동안 중요한 알림 알려줘';
        String? sentContent;

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChatInputField(
                onSend: (content) => sentContent = content,
              ),
            ),
          ),
        );

        await tester.enterText(find.byType(TextField), query);
        await tester.pump();

        await tester.tap(find.byIcon(Icons.send_rounded));
        await tester.pump();

        expect(sentContent, equals(query));
      });

      testWidgets(
          '미지원 기간 표현도 프론트에서 가공 없이 그대로 전송된다',
          (tester) async {
        const unsupportedQuery = '4월 20일부터 4월 22일까지 알림 요약해줘';
        String? sentContent;

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChatInputField(
                onSend: (content) => sentContent = content,
              ),
            ),
          ),
        );

        await tester.enterText(find.byType(TextField), unsupportedQuery);
        await tester.pump();

        await tester.tap(find.byIcon(Icons.send_rounded));
        await tester.pump();

        // 프론트에서 파싱/변환 없이 원문 그대로 전달
        expect(sentContent, equals(unsupportedQuery));
      });

      testWidgets('TextField가 maxLines null로 설정되어 긴 텍스트가 잘리지 않는다',
          (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChatInputField(onSend: (_) {}),
            ),
          ),
        );

        final textField = tester.widget<TextField>(find.byType(TextField));
        expect(textField.maxLines, isNull,
            reason: 'maxLines가 null이어야 긴 입력이 잘리지 않습니다');
      });

      testWidgets('비어 있는 입력은 전송되지 않는다', (tester) async {
        var sendCalled = false;

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChatInputField(onSend: (_) => sendCalled = true),
            ),
          ),
        );

        await tester.tap(find.byIcon(Icons.send_rounded));
        await tester.pump();

        expect(sendCalled, isFalse);
      });

      testWidgets('enabled=false일 때 전송 버튼이 비활성화된다', (tester) async {
        var sendCalled = false;

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChatInputField(
                onSend: (_) => sendCalled = true,
                enabled: false,
              ),
            ),
          ),
        );

        await tester.enterText(find.byType(TextField), '테스트');
        await tester.pump();

        await tester.tap(find.byIcon(Icons.send_rounded));
        await tester.pump();

        expect(sendCalled, isFalse);
      });

      testWidgets('기간 필터 전용 UI(DatePicker, RangePicker 등)가 존재하지 않는다',
          (tester) async {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: ChatInputField(onSend: (_) {}),
            ),
          ),
        );

        expect(find.byType(DatePickerDialog), findsNothing);
        expect(find.byType(DateRangePickerDialog), findsNothing);
        expect(find.byType(DropdownButton), findsNothing);
      });
    });

    // ---------------------------------------------------------------
    // 3 & 4. ChatMessageBubble: 기간 표현 포함 메시지가 원문 표시된다
    // ---------------------------------------------------------------

    group('ChatMessageBubble', () {
      Widget wrap(Widget child) => MaterialApp(
            home: Scaffold(body: child),
          );

      testWidgets('오늘 받은 알림 요약해줘 입력이 chat bubble에 원문 그대로 표시된다',
          (tester) async {
        const content = '오늘 받은 알림 요약해줘';

        final message = ChatMessageEntity(
          id: 1,
          role: MessageRole.user,
          content: content,
          createdAt: DateTime(2026, 4, 23),
        );

        await tester.pumpWidget(wrap(ChatMessageBubble(message: message)));
        await tester.pumpAndSettle();

        expect(find.text(content), findsOneWidget);
      });

      testWidgets('어제 GitHub 알림 정리해줘 입력이 chat bubble에 원문 그대로 표시된다',
          (tester) async {
        const content = '어제 GitHub 알림 정리해줘';

        final message = ChatMessageEntity(
          id: 2,
          role: MessageRole.user,
          content: content,
          createdAt: DateTime(2026, 4, 23),
        );

        await tester.pumpWidget(wrap(ChatMessageBubble(message: message)));
        await tester.pumpAndSettle();

        expect(find.text(content), findsOneWidget);
      });

      testWidgets('미지원 기간 표현도 chat bubble에 원문 그대로 표시된다',
          (tester) async {
        const content = '4월 20일부터 4월 22일까지 알림 요약해줘';

        final message = ChatMessageEntity(
          id: 3,
          role: MessageRole.user,
          content: content,
          createdAt: DateTime(2026, 4, 23),
        );

        await tester.pumpWidget(wrap(ChatMessageBubble(message: message)));
        await tester.pumpAndSettle();

        expect(find.text(content), findsOneWidget);
      });

      testWidgets('assistant 응답도 chat bubble에 원문 그대로 표시된다',
          (tester) async {
        const content = '최근 5시간 내에 받은 알림은 총 3건입니다.';

        final message = ChatMessageEntity(
          id: 4,
          role: MessageRole.assistant,
          content: content,
          createdAt: DateTime(2026, 4, 23),
        );

        await tester.pumpWidget(wrap(ChatMessageBubble(message: message)));
        await tester.pumpAndSettle();

        expect(find.text(content), findsOneWidget);
      });
    });
  });
}
