import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:go_router/go_router.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/auth/domain/auth_password_policy.dart';
import 'package:notio_app/features/auth/presentation/providers/signup_action_provider.dart';
import 'package:notio_app/features/auth/presentation/widgets/auth_screen_shell.dart';

class SignupScreen extends HookConsumerWidget {
  const SignupScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final nameController = useTextEditingController();
    final emailController = useTextEditingController();
    final passwordController = useTextEditingController();
    final isPasswordVisible = useState(false);
    useListenable(passwordController);

    final signupState = ref.watch(signupActionNotifierProvider);
    final passwordValidation =
        AuthPasswordPolicy.validate(passwordController.text);
    final hasPasswordInput = passwordController.text.isNotEmpty;

    ref.listen<SignupActionState>(signupActionNotifierProvider,
        (previous, next) {
      if (next.isSuccess &&
          !(previous?.isSuccess ?? false) &&
          context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('회원가입이 완료되었습니다. 로그인 화면으로 이동합니다.'),
            backgroundColor: AppColors.success,
          ),
        );
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (context.mounted) {
            context.go(Routes.login);
          }
        });
      }
    });

    return AuthScreenShell(
      icon: Icons.person_add_alt_1_rounded,
      title: '새 계정 만들기',
      subtitle: '개발 알림을 요약하고 우선순위로 정리하는 개인 허브를 바로 시작합니다.',
      bottomAction: AuthSecondaryAction(
        label: '로그인으로 돌아가기',
        onPressed: () => context.go(Routes.login),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            '회원가입',
            style: AppTextStyles.headlineLarge.copyWith(color: AppColors.text1),
            textAlign: TextAlign.center,
          ),
          if (signupState.error != null) ...[
            const SizedBox(height: AppSpacing.s16),
            AuthStatusCard(
              message: signupState.error!,
              icon: Icons.error_outline,
              color: AppColors.error,
            ),
          ],
          if (signupState.isLoading) ...[
            const SizedBox(height: AppSpacing.s16),
            const AuthStatusCard(
              message: '계정을 생성하고 있습니다.',
              icon: Icons.hourglass_top_rounded,
              color: AppColors.info,
            ),
          ],
          const SizedBox(height: AppSpacing.s24),
          TextField(
            controller: nameController,
            style: AppTextStyles.bodyMedium,
            decoration: _decoration(
              '이름',
              '표시 이름을 입력하세요',
              Icons.badge_outlined,
            ),
          ),
          const SizedBox(height: AppSpacing.s16),
          TextField(
            controller: emailController,
            keyboardType: TextInputType.emailAddress,
            style: AppTextStyles.bodyMedium,
            decoration: _decoration(
              '이메일',
              'name@company.com',
              Icons.alternate_email_rounded,
            ),
          ),
          const SizedBox(height: AppSpacing.s16),
          TextField(
            controller: passwordController,
            obscureText: !isPasswordVisible.value,
            style: AppTextStyles.bodyMedium,
            decoration: _decoration(
              '비밀번호',
              '안전한 비밀번호를 입력하세요',
              Icons.lock_outline_rounded,
            ).copyWith(
              suffixIcon: IconButton(
                onPressed: () {
                  isPasswordVisible.value = !isPasswordVisible.value;
                },
                icon: Icon(
                  isPasswordVisible.value
                      ? Icons.visibility_off_rounded
                      : Icons.visibility_rounded,
                ),
              ),
            ),
          ),
          const SizedBox(height: AppSpacing.s12),
          _PasswordRequirementCard(
            validation: passwordValidation,
            isVisible: hasPasswordInput,
          ),
          const SizedBox(height: AppSpacing.s24),
          SizedBox(
            height: 52,
            child: FilledButton(
              onPressed: signupState.isLoading
                  ? null
                  : () {
                      final displayName = nameController.text.trim();
                      final email = emailController.text.trim();
                      final password = passwordController.text.trim();

                      if (displayName.isEmpty ||
                          email.isEmpty ||
                          password.isEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('이름, 이메일, 비밀번호를 모두 입력해주세요.'),
                            backgroundColor: AppColors.warning,
                          ),
                        );
                        return;
                      }

                      if (!passwordValidation.isValid) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(passwordValidation.message),
                            backgroundColor: AppColors.warning,
                          ),
                        );
                        return;
                      }

                      ref
                          .read(signupActionNotifierProvider.notifier)
                          .signup(email, password, displayName);
                    },
              child: signupState.isLoading
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('회원가입'),
            ),
          ),
        ],
      ),
    );
  }

  InputDecoration _decoration(String label, String hint, IconData icon) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
      prefixIcon: Icon(icon),
      filled: true,
      fillColor: AppColors.bg3.withValues(alpha: 0.72),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(18),
        borderSide: const BorderSide(color: AppColors.border2),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(18),
        borderSide: const BorderSide(color: AppColors.violet2, width: 1.5),
      ),
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(18)),
    );
  }
}

class _PasswordRequirementCard extends StatelessWidget {
  final AuthPasswordValidationResult validation;
  final bool isVisible;

  const _PasswordRequirementCard({
    required this.validation,
    required this.isVisible,
  });

  @override
  Widget build(BuildContext context) {
    final baseTextStyle = AppTextStyles.bodySmall.copyWith(
      color: AppColors.text2,
      height: 1.4,
    );

    return Container(
      padding: const EdgeInsets.all(AppSpacing.s16),
      decoration: BoxDecoration(
        color: AppColors.bg3.withValues(alpha: 0.55),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: validation.isValid
              ? AppColors.success.withValues(alpha: 0.4)
              : AppColors.border2,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '비밀번호 규칙',
            style: AppTextStyles.labelLarge.copyWith(color: AppColors.text1),
          ),
          const SizedBox(height: AppSpacing.s8),
          _PasswordRequirementRow(
            label: '8자 이상 100자 이하',
            isSatisfied: validation.hasMinLength && validation.hasMaxLength,
            isVisible: isVisible,
          ),
          const SizedBox(height: AppSpacing.s8),
          _PasswordRequirementRow(
            label: '영문 포함',
            isSatisfied: validation.hasLetter,
            isVisible: isVisible,
          ),
          const SizedBox(height: AppSpacing.s8),
          _PasswordRequirementRow(
            label: '숫자 포함',
            isSatisfied: validation.hasNumber,
            isVisible: isVisible,
          ),
          const SizedBox(height: AppSpacing.s8),
          _PasswordRequirementRow(
            label: '특수문자 포함',
            isSatisfied: validation.hasSpecialCharacter,
            isVisible: isVisible,
          ),
          if (!validation.isValid) ...[
            const SizedBox(height: AppSpacing.s8),
            Text(
              validation.message,
              style: baseTextStyle,
            ),
          ],
        ],
      ),
    );
  }
}

class _PasswordRequirementRow extends StatelessWidget {
  final String label;
  final bool isSatisfied;
  final bool isVisible;

  const _PasswordRequirementRow({
    required this.label,
    required this.isSatisfied,
    required this.isVisible,
  });

  @override
  Widget build(BuildContext context) {
    final isActive = isVisible && isSatisfied;
    final color = isActive ? AppColors.success : AppColors.text2;

    return Row(
      children: [
        Icon(
          isActive ? Icons.check_circle_rounded : Icons.radio_button_unchecked,
          size: 16,
          color: color,
        ),
        const SizedBox(width: AppSpacing.s8),
        Expanded(
          child: Text(
            label,
            style: AppTextStyles.bodySmall.copyWith(color: color),
          ),
        ),
      ],
    );
  }
}
