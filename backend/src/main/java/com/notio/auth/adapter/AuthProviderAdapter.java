package com.notio.auth.adapter;

import com.notio.auth.domain.AuthProvider;
import com.notio.auth.domain.AuthProviderState;
import com.notio.auth.dto.OAuthCallbackResponse;
import com.notio.auth.dto.OAuthExchangeResponse;

public interface AuthProviderAdapter {

    AuthProvider supports();

    String buildAuthorizationUrl(AuthProviderState providerState);

    OAuthCallbackResponse handleCallback(String authorizationCode, AuthProviderState providerState);

    OAuthExchangeResponse exchangeAuthorizationCode(String authorizationCode, AuthProviderState providerState);
}
