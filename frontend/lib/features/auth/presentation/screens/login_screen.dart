import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:go_router/go_router.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/auth/domain/auth_input_policy.dart';
import 'package:notio_app/features/auth/presentation/providers/login_action_provider.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_action_provider.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_config_provider.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_entry_strategy_provider.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_platform_provider.dart';
import 'package:notio_app/features/auth/presentation/widgets/auth_screen_shell.dart';

class LoginScreen extends HookConsumerWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final emailController = useTextEditingController();
    final passwordController = useTextEditingController();
    final isPasswordVisible = useState(false);

    final loginState = ref.watch(loginActionNotifierProvider);
    final socialState = ref.watch(socialLoginActionNotifierProvider);
    final socialConfigs = ref.watch(socialLoginProviderConfigsProvider);
    final currentPlatform = ref.watch(currentAuthPlatformProvider);
    final redirectUri = ref.watch(oauthRedirectUriProvider);

    ref.listen<LoginActionState>(loginActionNotifierProvider, (previous, next) {
      if (next.isSuccess && !(previous?.isSuccess ?? false)) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (context.mounted) {
            context.go(Routes.notifications);
          }
        });
      }
    });

    ref.listen<SocialLoginActionState>(socialLoginActionNotifierProvider,
        (previous, next) async {
      final authorizationUrl = next.authorizationUrl;
      final activeProvider = next.activeProvider;
      if (authorizationUrl != null &&
          authorizationUrl != previous?.authorizationUrl &&
          activeProvider != null) {
        final strategy = ref.read(
          socialLoginEntryStrategyProvider(activeProvider),
        );
        final launched =
            await strategy.launchAuthorizationUrl(authorizationUrl);

        if (!launched && context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('소셜 로그인 페이지를 열지 못했습니다.'),
              backgroundColor: AppColors.error,
            ),
          );
        }

        ref.read(socialLoginActionNotifierProvider.notifier).reset();
      }
    });

    final statusMessage = loginState.error ?? socialState.error;
    final isBusy = loginState.isLoading || socialState.isLoading;

    return AuthScreenShell(
      icon: Icons.notifications_active_rounded,
      title: 'Notio 로그인',
      subtitle: '실시간 알림 흐름을 한 곳에서 관리하고, 바로 다음 액션으로 연결합니다.',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            '계정으로 계속하기',
            style: AppTextStyles.headlineLarge.copyWith(color: AppColors.text1),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: AppSpacing.s8),
          Text(
            '이메일 로그인 또는 연결된 소셜 계정으로 접근할 수 있습니다.',
            style: AppTextStyles.bodySmall.copyWith(color: AppColors.text2),
            textAlign: TextAlign.center,
          ),
          if (statusMessage != null) ...[
            const SizedBox(height: AppSpacing.s16),
            AuthStatusCard(
              message: statusMessage,
              icon: Icons.error_outline,
              color: AppColors.error,
            ),
          ],
          if (socialState.isLoading) ...[
            const SizedBox(height: AppSpacing.s16),
            const AuthStatusCard(
              message: '소셜 로그인 페이지를 준비하고 있습니다.',
              icon: Icons.open_in_browser,
              color: AppColors.info,
            ),
          ],
          const SizedBox(height: AppSpacing.s24),
          TextField(
            controller: emailController,
            keyboardType: TextInputType.emailAddress,
            autofillHints: const [AutofillHints.username, AutofillHints.email],
            style: AppTextStyles.bodyMedium,
            decoration: _inputDecoration(
              labelText: '이메일',
              hintText: 'name@company.com',
              icon: Icons.alternate_email_rounded,
            ),
          ),
          const SizedBox(height: AppSpacing.s16),
          TextField(
            controller: passwordController,
            obscureText: !isPasswordVisible.value,
            autofillHints: const [AutofillHints.password],
            style: AppTextStyles.bodyMedium,
            decoration: _inputDecoration(
              labelText: '비밀번호',
              hintText: '비밀번호를 입력하세요',
              icon: Icons.lock_outline_rounded,
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
              onPressed: isBusy
                  ? null
                  : () {
                      final email = emailController.text.trim();
                      final password = passwordController.text.trim();
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

                      if (password.isEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('비밀번호를 입력해주세요.'),
                            backgroundColor: AppColors.warning,
                          ),
                        );
                        return;
                      }

                      ref
                          .read(loginActionNotifierProvider.notifier)
                          .login(email, password);
                    },
              child: loginState.isLoading
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('로그인'),
            ),
          ),
          const SizedBox(height: AppSpacing.s16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              AuthSecondaryAction(
                label: '회원가입',
                onPressed: () => context.go(Routes.signup),
              ),
              AuthSecondaryAction(
                label: '아이디 찾기',
                onPressed: () => context.go(Routes.findId),
              ),
              AuthSecondaryAction(
                label: '비밀번호 찾기',
                onPressed: () => context.go(Routes.resetPasswordRequest),
              ),
            ],
          ),
          if (socialConfigs.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.s24),
            Row(
              children: [
                Expanded(
                  child:
                      Divider(color: AppColors.border2.withValues(alpha: 0.7)),
                ),
                Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: AppSpacing.s12),
                  child: Text(
                    '소셜 로그인',
                    style: AppTextStyles.labelMedium.copyWith(
                      color: AppColors.text2,
                    ),
                  ),
                ),
                Expanded(
                  child:
                      Divider(color: AppColors.border2.withValues(alpha: 0.7)),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.s16),
            ...socialConfigs.map(
              (config) => Padding(
                padding: const EdgeInsets.only(bottom: AppSpacing.s12),
                child: SizedBox(
                  height: 52,
                  child: FilledButton.tonal(
                    onPressed: isBusy
                        ? null
                        : () {
                            ref
                                .read(
                                  socialLoginActionNotifierProvider.notifier,
                                )
                                .startSocialLogin(
                                  config.provider,
                                  currentPlatform,
                                  redirectUri,
                                );
                          },
                    style: FilledButton.styleFrom(
                      backgroundColor: config.backgroundColor,
                      foregroundColor: config.foregroundColor,
                      textStyle: AppTextStyles.labelLarge.copyWith(
                        color: config.foregroundColor,
                      ),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(config.icon, size: 18),
                        const SizedBox(width: AppSpacing.s8),
                        Text('${config.label}로 계속하기'),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  InputDecoration _inputDecoration({
    required String labelText,
    required String hintText,
    required IconData icon,
  }) {
    return InputDecoration(
      labelText: labelText,
      hintText: hintText,
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
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(18),
      ),
    );
  }
}
