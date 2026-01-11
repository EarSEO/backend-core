package com.earseo.core.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://apis.data.go.kr")
                .build();
    }
}