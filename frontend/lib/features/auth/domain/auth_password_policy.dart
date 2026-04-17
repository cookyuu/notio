class AuthPasswordPolicy {
  AuthPasswordPolicy._();

  static const int minLength = 8;
  static const int maxLength = 100;

  static final RegExp _letterPattern = RegExp(r'[A-Za-z]');
  static final RegExp _numberPattern = RegExp(r'\d');
  static final RegExp _specialCharacterPattern = RegExp(r'[^A-Za-z0-9]');

  static AuthPasswordValidationResult validate(String password) {
    final hasMinLength = password.length >= minLength;
    final hasMaxLength = password.length <= maxLength;
    final hasLetter = _letterPattern.hasMatch(password);
    final hasNumber = _numberPattern.hasMatch(password);
    final hasSpecialCharacter = _specialCharacterPattern.hasMatch(password);

    return AuthPasswordValidationResult(
      hasMinLength: hasMinLength,
      hasMaxLength: hasMaxLength,
      hasLetter: hasLetter,
      hasNumber: hasNumber,
      hasSpecialCharacter: hasSpecialCharacter,
    );
  }
}

class AuthPasswordValidationResult {
  final bool hasMinLength;
  final bool hasMaxLength;
  final bool hasLetter;
  final bool hasNumber;
  final bool hasSpecialCharacter;

  const AuthPasswordValidationResult({
    required this.hasMinLength,
    required this.hasMaxLength,
    required this.hasLetter,
    required this.hasNumber,
    required this.hasSpecialCharacter,
  });

  bool get isValid {
    return hasMinLength &&
        hasMaxLength &&
        hasLetter &&
        hasNumber &&
        hasSpecialCharacter;
  }

  String get message {
    return '비밀번호는 8자 이상 100자 이하이며 영문, 숫자, 특수문자를 포함해야 합니다.';
  }
}
