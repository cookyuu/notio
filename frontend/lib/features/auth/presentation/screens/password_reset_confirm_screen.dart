import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:go_router/go_router.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/auth/presentation/providers/password_reset_confirm_provider.dart';
import 'package:notio_app/features/auth/presentation/widgets/auth_screen_shell.dart';

class PasswordResetConfirmScreen extends HookConsumerWidget {
  const PasswordResetConfirmScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tokenController = useTextEditingController();
    final passwordController = useTextEditingController();
    final isPasswordVisible = useState(false);
    final state = ref.watch(passwordResetConfirmNotifierProvider);

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
          const SizedBox(height: AppSpacing.s24),
          SizedBox(
            height: 52,
            child: FilledButton(
              onPressed: state.isLoading
                  ? null
                  : () {
                      final token = tokenController.text.trim();
                      final password = passwordController.text.trim();
                      if (token.isEmpty || password.isEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('토큰과 새 비밀번호를 모두 입력해주세요.'),
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
