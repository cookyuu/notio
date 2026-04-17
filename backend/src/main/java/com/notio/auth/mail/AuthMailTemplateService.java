package com.notio.auth.mail;

import com.notio.auth.config.AuthProperties;
import com.notio.auth.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthMailTemplateService {

    private final AuthProperties authProperties;

    public AuthMailMessage buildFindIdMessage(final User user) {
        final String displayName = resolveDisplayName(user);
        final String subject = "[Notio] 가입 이메일 안내";
        final String body = """
                %s님,

                Notio 로그인 이메일 안내 요청이 접수되었습니다.
                가입 이메일: %s

                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(displayName, user.getPrimaryEmail());

        return new AuthMailMessage(user.getPrimaryEmail(), subject, body, List.of());
    }

    public AuthMailMessage buildPasswordResetMessage(final User user, final String rawToken) {
        final String displayName = resolveDisplayName(user);
        final String subject = "[Notio] 비밀번호 재설정 안내";
        final long ttlMinutes = authProperties.getPasswordReset().getTokenTtl().toMinutes();
        final String resetLink = buildResetLink(rawToken);
        final String body = """
                %s님,

                Notio 비밀번호 재설정 요청이 접수되었습니다.
                아래 재설정 토큰을 사용해 비밀번호를 변경해 주세요.%s

                reset_token: %s

                이 토큰은 %d분 뒤 만료됩니다.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(displayName, resetLink, rawToken, ttlMinutes);

        return new AuthMailMessage(user.getPrimaryEmail(), subject, body, List.of(rawToken));
    }

    private String buildResetLink(final String rawToken) {
        final String resetLinkBaseUrl = authProperties.getMail().getResetLinkBaseUrl();
        if (resetLinkBaseUrl == null || resetLinkBaseUrl.isBlank()) {
            return "";
        }
        return """

                아래 링크에서도 비밀번호를 재설정할 수 있습니다.
                %s?token=%s
                """.formatted(resetLinkBaseUrl.trim(), rawToken);
    }

    private String resolveDisplayName(final User user) {
        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            return "Notio 사용자";
        }
        return user.getDisplayName().trim();
    }
}
