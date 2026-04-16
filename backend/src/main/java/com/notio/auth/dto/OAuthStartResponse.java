package com.notio.auth.dto;

import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthStartResponse {

    private AuthProvider provider;
    private AuthPlatform platform;
    private String state;
    private String authorizationUrl;
    private OffsetDateTime expiresAt;
}
