package com.notio.auth.dto;

import com.notio.auth.domain.AuthPlatform;
import com.notio.auth.domain.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthCallbackResponse {

    private AuthProvider provider;
    private AuthPlatform platform;
    private String state;
    private String redirectUri;
    private String message;
}
