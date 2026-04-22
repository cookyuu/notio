package com.notio.common.config;

import com.notio.common.config.properties.NotioAiProperties;
import com.notio.common.config.properties.NotioRagProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        NotioAiProperties.class,
        NotioRagProperties.class
})
public class SpringAiConfig {
}
