package com.notio.auth.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notio.auth")
@Getter
@Setter
public class AuthProperties {

    private PasswordReset passwordReset = new PasswordReset();
    private Mail mail = new Mail();
    private OAuth oauth = new OAuth();

    @Getter
    @Setter
    public static class PasswordReset {
        private Duration tokenTtl = Duration.ofMinutes(30);
    }

    @Getter
    @Setter
    public static class Mail {
        private DeliveryMode deliveryMode = DeliveryMode.NOOP;
        private String fromAddress = "no-reply@notio.local";
        private String fromName = "Notio";
        private String resetLinkBaseUrl;
    }

    @Getter
    @Setter
    public static class OAuth {
        private Duration stateTtl = Duration.ofMinutes(5);
        private Provider google = new Provider();
        private Provider apple = new Provider();
        private Provider kakao = new Provider();
        private Provider naver = new Provider();
    }

    @Getter
    @Setter
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String webRedirectUri;
        private String mobileRedirectUri;
    }

    public enum DeliveryMode {
        LOG,
        NOOP
    }
}
