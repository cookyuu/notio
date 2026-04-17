import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:go_router/go_router.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/auth/domain/auth_input_policy.dart';
import 'package:notio_app/features/auth/presentation/providers/find_id_action_provider.dart';
import 'package:notio_app/features/auth/presentation/widgets/auth_screen_shell.dart';

class FindIdScreen extends HookConsumerWidget {
  const FindIdScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final emailController = useTextEditingController();
    final state = ref.watch(findIdActionNotifierProvider);

    return AuthScreenShell(
      icon: Icons.search_rounded,
      title: '아이디 찾기',
      subtitle: '가입한 이메일을 기준으로 계정 정보를 다시 확인할 수 있습니다.',
      bottomAction: AuthSecondaryAction(
        label: '로그인으로 돌아가기',
        onPressed: () => context.go(Routes.login),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            '이메일 확인',
            style: AppTextStyles.headlineLarge.copyWith(color: AppColors.text1),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '계정 존재 여부를 직접 노출하지 않도록 안내 메시지로 결과를 보여줍니다.',
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
              icon: state.isSuccess
                  ? Icons.mark_email_read_outlined
                  : Icons.info_outline,
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

                      ref.read(findIdActionNotifierProvider.notifier).findId(
                            email,
                          );
                    },
              child: state.isLoading
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('아이디 찾기'),
            ),
          ),
        ],
      ),
    );
  }

  InputDecoration _decoration() {
    return InputDecoration(
      labelText: '가입 이메일',
      hintText: 'name@company.com',
      prefixIcon: const Icon(Icons.alternate_email_rounded),
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
