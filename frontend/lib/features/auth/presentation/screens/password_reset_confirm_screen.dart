import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:go_router/go_router.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/auth/domain/auth_input_policy.dart';
import 'package:notio_app/features/auth/domain/auth_password_policy.dart';
import 'package:notio_app/features/auth/domain/auth_route_policy.dart';
import 'package:notio_app/features/auth/presentation/providers/password_reset_confirm_provider.dart';
import 'package:notio_app/features/auth/presentation/widgets/auth_screen_shell.dart';

class PasswordResetConfirmScreen extends HookConsumerWidget {
  const PasswordResetConfirmScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final routeState = GoRouterState.of(context);
    final tokenController = useTextEditingController();
    final passwordController = useTextEditingController();
    final confirmPasswordController = useTextEditingController();
    final isPasswordVisible = useState(false);
    final isConfirmPasswordVisible = useState(false);
    final state = ref.watch(passwordResetConfirmNotifierProvider);
    useListenable(passwordController);
    useListenable(confirmPasswordController);

    final initialToken = AuthRoutePolicy.extractResetToken(
      uri: routeState.uri,
      pathToken: routeState.pathParameters['token'],
    );
    final passwordValidation =
        AuthPasswordPolicy.validate(passwordController.text);
    final hasPasswordInput = passwordController.text.isNotEmpty;
    final confirmPasswordError = AuthInputPolicy.validatePasswordConfirmation(
      password: passwordController.text,
      confirmation: confirmPasswordController.text,
    );
    final showConfirmPasswordError = confirmPasswordController.text.isNotEmpty;

    useEffect(() {
      if ((tokenController.text.isEmpty) && initialToken != null) {
        tokenController.text = initialToken;
      }
      return null;
    }, [initialToken]);

    ref.listen<PasswordResetConfirmState>(
      passwordResetConfirmNotifierProvider,
      (previous, next) {
        if (next.isSuccess &&
            !(previous?.isSuccess ?? false) &&
            context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('비밀번호가 변경되었습니다. 로그인 화면으로 이동합니다.'),
              backgroundColor: AppColors.success,
            ),
          );
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (context.mounted) {
              context.go(Routes.login);
            }
          });
        }
      },
    );

    return AuthScreenShell(
      icon: Icons.verified_user_outlined,
      title: '새 비밀번호 설정',
      subtitle: '이메일로 받은 토큰을 입력하고 새로운 비밀번호로 교체합니다.',
      bottomAction: AuthSecondaryAction(
        label: '로그인으로 돌아가기',
        onPressed: () => context.go(Routes.login),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            '비밀번호 재설정',
            style: AppTextStyles.headlineLarge.copyWith(color: AppColors.text1),
            textAlign: TextAlign.center,
          ),
          if (state.error != null) ...[
            const SizedBox(height: AppSpacing.s16),
            AuthStatusCard(
              message: state.error!,
              icon: Icons.error_outline,
              color: AppColors.error,
            ),
          ],
          if (state.message != null) ...[
            const SizedBox(height: AppSpacing.s16),
            AuthStatusCard(
              message: state.message!,
              icon: state.isSuccess ? Icons.check_circle_outline : Icons.info,
              color: state.isSuccess ? AppColors.success : AppColors.info,
            ),
          ],
          const SizedBox(height: AppSpacing.s24),
          TextField(
            controller: tokenController,
            style: AppTextStyles.bodyMedium,
            decoration: _decoration(
              '재설정 토큰',
              '메일에서 받은 토큰',
              Icons.key_outlined,
            ),
          ),
          const SizedBox(height: AppSpacing.s16),
          TextField(
            controller: passwordController,
            obscureText: !isPasswordVisible.value,
            style: AppTextStyles.bodyMedium,
            decoration: _decoration(
              '새 비밀번호',
              '새 비밀번호를 입력하세요',
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
          const SizedBox(height: AppSpacing.s16),
          TextField(
            controller: confirmPasswordController,
            obscureText: !isConfirmPasswordVisible.value,
            style: AppTextStyles.bodyMedium,
            decoration: _decoration(
              '새 비밀번호 확인',
              '새 비밀번호를 다시 입력하세요',
              Icons.verified_user_outlined,
            ).copyWith(
              suffixIcon: IconButton(
                onPressed: () {
                  isConfirmPasswordVisible.value =
                      !isConfirmPasswordVisible.value;
                },
                icon: Icon(
                  isConfirmPasswordVisible.value
                      ? Icons.visibility_off_rounded
                      : Icons.visibility_rounded,
                ),
              ),
              errorText: showConfirmPasswordError ? confirmPasswordError : null,
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
              onPressed: state.isLoading
                  ? null
                  : () {
                      final token = tokenController.text.trim();
                      final password = passwordController.text.trim();
                      final confirmPassword =
                          confirmPasswordController.text.trim();
                      if (token.isEmpty ||
                          password.isEmpty ||
                          confirmPassword.isEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('토큰과 새 비밀번호를 모두 입력해주세요.'),
                            backgroundColor: AppColors.warning,
                          ),
                        );
                        return;
                      }

                      final passwordError =
                          AuthInputPolicy.validatePassword(password);
                      if (passwordError != null) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(passwordError),
                            backgroundColor: AppColors.warning,
                          ),
                        );
                        return;
                      }

                      final confirmationError =
                          AuthInputPolicy.validatePasswordConfirmation(
                        password: password,
                        confirmation: confirmPassword,
                      );
                      if (confirmationError != null) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(confirmationError),
                            backgroundColor: AppColors.warning,
                          ),
                        );
                        return;
                      }

                      ref
                          .read(passwordResetConfirmNotifierProvider.notifier)
                          .confirmPasswordReset(token, password);
                    },
              child: state.isLoading
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('비밀번호 변경'),
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
  const _PasswordRequirementCard({
    required this.validation,
    required this.isVisible,
  });

  final AuthPasswordValidationResult validation;
  final bool isVisible;

  @override
  Widget build(BuildContext context) {
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
              style: AppTextStyles.bodySmall.copyWith(
                color: AppColors.text2,
                height: 1.4,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _PasswordRequirementRow extends StatelessWidget {
  const _PasswordRequirementRow({
    required this.label,
    required this.isSatisfied,
    required this.isVisible,
  });

  final String label;
  final bool isSatisfied;
  final bool isVisible;

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
