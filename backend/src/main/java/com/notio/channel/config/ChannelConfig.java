package com.notio.channel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
public class ChannelConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
