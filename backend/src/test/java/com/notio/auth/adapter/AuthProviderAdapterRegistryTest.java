package com.notio.auth.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.notio.auth.domain.AuthProvider;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthProviderAdapterRegistryTest {

    @Test
    void getRejectsUnsupportedProviderWhenNoAdapterExists() {
        final AuthProviderAdapterRegistry registry = new AuthProviderAdapterRegistry(List.of());

        assertThatThrownBy(() -> registry.get(AuthProvider.GOOGLE))
                .isInstanceOf(NotioException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_PROVIDER_UNSUPPORTED);
    }

    @Test
    void getRejectsLocalProviderEvenWhenRegistryIsQueried() {
        final AuthProviderAdapterRegistry registry = new AuthProviderAdapterRegistry(List.of());

        assertThatThrownBy(() -> registry.get(AuthProvider.LOCAL))
                .isInstanceOf(NotioException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_PROVIDER_UNSUPPORTED);
    }
}
