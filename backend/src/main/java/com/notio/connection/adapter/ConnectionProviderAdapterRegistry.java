package com.notio.connection.adapter;

import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.connection.domain.ConnectionProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ConnectionProviderAdapterRegistry {

    private final Map<ConnectionProvider, ConnectionProviderAdapter> adapters;

    public ConnectionProviderAdapterRegistry(final List<ConnectionProviderAdapter> adapters) {
        this.adapters = new EnumMap<>(ConnectionProvider.class);
        for (ConnectionProviderAdapter adapter : adapters) {
            this.adapters.put(adapter.supports(), adapter);
        }
    }

    public ConnectionProviderAdapter get(final ConnectionProvider provider) {
        final ConnectionProviderAdapter adapter = adapters.get(provider);
        if (adapter == null) {
            throw new NotioException(ErrorCode.CONNECTION_PROVIDER_UNSUPPORTED);
        }
        return adapter;
    }
}
