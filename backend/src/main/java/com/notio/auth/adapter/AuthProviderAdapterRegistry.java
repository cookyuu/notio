package com.notio.auth.adapter;

import com.notio.auth.domain.AuthProvider;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuthProviderAdapterRegistry {

    private final Map<AuthProvider, AuthProviderAdapter> adapters;

    public AuthProviderAdapterRegistry(final List<AuthProviderAdapter> adapters) {
        this.adapters = new EnumMap<>(AuthProvider.class);
        for (AuthProviderAdapter adapter : adapters) {
            this.adapters.put(adapter.supports(), adapter);
        }
    }

    public AuthProviderAdapter get(final AuthProvider provider) {
        final AuthProviderAdapter adapter = adapters.get(provider);
        if (adapter == null || provider == AuthProvider.LOCAL) {
            throw new NotioException(ErrorCode.AUTH_PROVIDER_UNSUPPORTED);
        }
        return adapter;
    }
}
