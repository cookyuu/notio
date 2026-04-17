import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/auth/data/models/signup_response.dart';

void main() {
  group('SignupResponse', () {
    test('parses message-only signup response', () {
      final response = SignupResponse.fromJson({
        'message': '회원가입이 완료되었습니다.',
      });

      expect(response.message, '회원가입이 완료되었습니다.');
    });
  });
}
