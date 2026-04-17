import 'package:flutter_test/flutter_test.dart';
import 'package:notio_app/features/auth/domain/auth_password_policy.dart';

void main() {
  group('AuthPasswordPolicy', () {
    test('accepts password meeting all requirements', () {
      final result = AuthPasswordPolicy.validate('Password123!');

      expect(result.isValid, isTrue);
    });

    test('rejects password shorter than minimum length', () {
      final result = AuthPasswordPolicy.validate('Pw1!');

      expect(result.hasMinLength, isFalse);
      expect(result.isValid, isFalse);
    });

    test('rejects password without letter', () {
      final result = AuthPasswordPolicy.validate('12345678!');

      expect(result.hasLetter, isFalse);
      expect(result.isValid, isFalse);
    });

    test('rejects password without number', () {
      final result = AuthPasswordPolicy.validate('Password!');

      expect(result.hasNumber, isFalse);
      expect(result.isValid, isFalse);
    });

    test('rejects password without special character', () {
      final result = AuthPasswordPolicy.validate('Password123');

      expect(result.hasSpecialCharacter, isFalse);
      expect(result.isValid, isFalse);
    });

    test('rejects password longer than maximum length', () {
      final overLimitPassword = 'Aa1!${'b' * 97}';
      final result = AuthPasswordPolicy.validate(overLimitPassword);

      expect(result.hasMaxLength, isFalse);
      expect(result.isValid, isFalse);
    });
  });
}
