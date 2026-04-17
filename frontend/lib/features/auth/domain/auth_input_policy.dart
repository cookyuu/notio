import 'package:notio_app/features/auth/domain/auth_password_policy.dart';

class AuthInputPolicy {
  AuthInputPolicy._();

  static final RegExp _emailPattern = RegExp(
    r'^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$',
    caseSensitive: false,
  );

  static const String genericFindIdSuccessMessage =
      '입력한 이메일로 가입 정보 안내를 전송했습니다.';
  static const String genericPasswordResetRequestSuccessMessage =
      '비밀번호 재설정 안내를 전송했습니다.';

  static bool isValidEmail(String email) {
    return _emailPattern.hasMatch(email.trim());
  }

  static String? validateEmail(String email) {
    if (email.trim().isEmpty) {
      return '이메일을 입력해주세요.';
    }

    if (!isValidEmail(email)) {
      return '올바른 이메일 형식을 입력해주세요.';
    }

    return null;
  }

  static String? validatePassword(String password) {
    if (password.isEmpty) {
      return '비밀번호를 입력해주세요.';
    }

    final result = AuthPasswordPolicy.validate(password);
    if (!result.isValid) {
      return result.message;
    }

    return null;
  }

  static String? validatePasswordConfirmation({
    required String password,
    required String confirmation,
  }) {
    if (confirmation.isEmpty) {
      return '비밀번호 확인을 입력해주세요.';
    }

    if (password != confirmation) {
      return '비밀번호가 일치하지 않습니다.';
    }

    return null;
  }
}
