package com.notio.common.config;

import com.notio.common.metrics.NotioMetricsTagPolicy;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public NotioMetricsTagPolicy notioMetricsTagPolicy() {
        return new NotioMetricsTagPolicy();
    }

    @Bean
    public MeterFilter notioMetricsTagFilter(final NotioMetricsTagPolicy tagPolicy) {
        return new MeterFilter() {
            @Override
            public Meter.Id map(final Meter.Id id) {
                if (!id.getName().startsWith("notio_")) {
                    return id;
                }
                return id.replaceTags(tagPolicy.sanitize(id.getTags()));
            }
        };
    }
}
