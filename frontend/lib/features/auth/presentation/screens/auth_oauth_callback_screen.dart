import 'package:flutter/material.dart';
import 'package:flutter_hooks/flutter_hooks.dart';
import 'package:go_router/go_router.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:notio_app/core/constants/app_spacing.dart';
import 'package:notio_app/core/router/routes.dart';
import 'package:notio_app/core/theme/app_colors.dart';
import 'package:notio_app/core/theme/app_text_styles.dart';
import 'package:notio_app/features/auth/domain/entities/auth_provider.dart';
import 'package:notio_app/features/auth/presentation/providers/social_login_action_provider.dart';
import 'package:notio_app/features/auth/presentation/widgets/auth_screen_shell.dart';

class AuthOAuthCallbackScreen extends HookConsumerWidget {
  const AuthOAuthCallbackScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final socialState = ref.watch(socialLoginActionNotifierProvider);
    final query = GoRouterState.of(context).uri.queryParameters;
    final handled = useState(false);

    useEffect(() {
      if (handled.value) {
        return null;
      }

      handled.value = true;

      final provider = _parseProvider(query['provider']);
      final code = query['code'];
      final state = query['state'];

      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (query['error'] != null) {
          ref.read(socialLoginActionNotifierProvider.notifier).reset();
          return;
        }

        if (provider != null && code != null && state != null) {
          ref
              .read(socialLoginActionNotifierProvider.notifier)
              .exchangeSocialLogin(provider, code, state);
        }
      });

      return null;
    }, const []);

    ref.listen<SocialLoginActionState>(socialLoginActionNotifierProvider,
        (previous, next) {
      if (next.isSuccess && !(previous?.isSuccess ?? false)) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (context.mounted) {
            context.go(Routes.notifications);
          }
        });
      }
    });

    final callbackError = query['error_description'] ??
        query['error'] ??
        socialState.error ??
        _missingParamMessage(query);

    return AuthScreenShell(
      icon: Icons.swap_horiz_rounded,
      title: '소셜 로그인 연결 중',
      subtitle: '인증 응답을 확인하고 Notio 세션으로 전환하고 있습니다.',
      bottomAction: AuthSecondaryAction(
        label: '로그인으로 돌아가기',
        onPressed: () => context.go(Routes.login),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'OAuth Callback',
            textAlign: TextAlign.center,
            style: AppTextStyles.headlineLarge.copyWith(color: AppColors.text1),
          ),
          const SizedBox(height: AppSpacing.s20),
          if (callbackError != null)
            AuthStatusCard(
              message: callbackError,
              icon: Icons.error_outline,
              color: AppColors.error,
            )
          else if (socialState.isSuccess)
            const AuthStatusCard(
              message: '로그인이 완료되었습니다. 알림 화면으로 이동합니다.',
              icon: Icons.check_circle_outline,
              color: AppColors.success,
            )
          else
            const AuthStatusCard(
              message: '인가 코드를 토큰으로 교환하고 있습니다.',
              icon: Icons.sync_rounded,
              color: AppColors.info,
            ),
          const SizedBox(height: AppSpacing.s24),
          Center(
            child: callbackError == null && !socialState.isSuccess
                ? const CircularProgressIndicator()
                : const SizedBox.shrink(),
          ),
        ],
      ),
    );
  }

  AuthProvider? _parseProvider(String? raw) {
    if (raw == null || raw.isEmpty) {
      return null;
    }

    for (final provider in AuthProvider.values) {
      if (provider.name.toLowerCase() == raw.toLowerCase()) {
        return provider;
      }
    }
    return null;
  }

  String? _missingParamMessage(Map<String, String> query) {
    if (query['error'] != null) {
      return null;
    }

    if (_parseProvider(query['provider']) == null) {
      return '소셜 로그인 제공자 정보가 없어 인증을 완료할 수 없습니다.';
    }

    if (query['code'] == null || query['state'] == null) {
      return '인가 코드 또는 상태 값이 누락되어 인증을 완료할 수 없습니다.';
    }

    return null;
  }
}
