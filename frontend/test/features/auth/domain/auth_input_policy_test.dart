import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/auth/domain/auth_input_policy.dart';

void main() {
  group('AuthInputPolicy', () {
    test('accepts valid email format', () {
      expect(AuthInputPolicy.validateEmail('dev@notio.app'), isNull);
    });

    test('rejects invalid email format', () {
      expect(
        AuthInputPolicy.validateEmail('invalid-email'),
        '올바른 이메일 형식을 입력해주세요.',
      );
    });

    test('rejects password shorter than minimum length', () {
      expect(
        AuthInputPolicy.validatePassword('Pw1!'),
        '비밀번호는 8자 이상 100자 이하이며 영문, 숫자, 특수문자를 포함해야 합니다.',
      );
    });

    test('rejects password confirmation mismatch', () {
      expect(
        AuthInputPolicy.validatePasswordConfirmation(
          password: 'Password123!',
          confirmation: 'Password123?',
        ),
        '비밀번호가 일치하지 않습니다.',
      );
    });

    test('exposes generic success messages for account recovery flows', () {
      expect(
        AuthInputPolicy.genericFindIdSuccessMessage,
        '입력한 이메일로 가입 정보 안내를 전송했습니다.',
      );
      expect(
        AuthInputPolicy.genericPasswordResetRequestSuccessMessage,
        '비밀번호 재설정 안내를 전송했습니다.',
      );
    });
  });
}
