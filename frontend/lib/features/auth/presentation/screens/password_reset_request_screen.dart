import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:go_router/go_router.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/auth/domain/auth_input_policy.dart';
import 'package:notio_app/features/auth/presentation/providers/password_reset_request_provider.dart';
import 'package:notio_app/features/auth/presentation/widgets/auth_screen_shell.dart';

class PasswordResetRequestScreen extends HookConsumerWidget {
  const PasswordResetRequestScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final emailController = useTextEditingController();
    final state = ref.watch(passwordResetRequestNotifierProvider);

    return AuthScreenShell(
      icon: Icons.lock_reset_rounded,
      title: '비밀번호 찾기',
      subtitle: '비밀번호 재설정 링크를 발급해 다시 접근할 수 있게 준비합니다.',
      bottomAction: AuthSecondaryAction(
        label: '로그인으로 돌아가기',
        onPressed: () => context.go(Routes.login),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            '재설정 링크 요청',
            style: AppTextStyles.headlineLarge.copyWith(color: AppColors.text1),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '보안상 계정 존재 여부와 관계없이 동일한 성공 메시지를 노출하는 흐름을 고려했습니다.',
            textAlign: TextAlign.center,
            style: AppTextStyles.bodySmall.copyWith(color: AppColors.text2),
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
              icon: state.isSuccess ? Icons.send_rounded : Icons.info_outline,
              color: state.isSuccess ? AppColors.success : AppColors.info,
            ),
          ],
          const SizedBox(height: AppSpacing.s24),
          TextField(
            controller: emailController,
            keyboardType: TextInputType.emailAddress,
            style: AppTextStyles.bodyMedium,
            decoration: _decoration(),
          ),
          const SizedBox(height: AppSpacing.s24),
          SizedBox(
            height: 52,
            child: FilledButton(
              onPressed: state.isLoading
                  ? null
                  : () {
                      final email = emailController.text.trim();
                      final emailError = AuthInputPolicy.validateEmail(email);
                      if (emailError != null) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(emailError),
                            backgroundColor: AppColors.warning,
                          ),
                        );
                        return;
                      }

                      ref
                          .read(passwordResetRequestNotifierProvider.notifier)
                          .requestPasswordReset(email);
                    },
              child: state.isLoading
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('재설정 링크 보내기'),
            ),
          ),
          const SizedBox(height: AppSpacing.s16),
          OutlinedButton(
            onPressed: () => context.go(Routes.resetPasswordConfirm),
            style: OutlinedButton.styleFrom(
              side: const BorderSide(color: AppColors.border2),
              foregroundColor: AppColors.text2,
              minimumSize: const Size.fromHeight(48),
            ),
            child: const Text('이미 토큰이 있다면 새 비밀번호 설정'),
          ),
        ],
      ),
    );
  }

  InputDecoration _decoration() {
    return InputDecoration(
      labelText: '가입 이메일',
      hintText: 'name@company.com',
      prefixIcon: const Icon(Icons.email_outlined),
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
