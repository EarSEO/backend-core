package com.earseo.core.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * OpenAPI 기본 정보 설정
     * Swagger 상단 노출
     */
    @Bean
    public OpenAPI defaultOpenAPI(@Value("${spring.application.name}") String appName) {
        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .components(new Components())
                .info(new Info()
                        .title(appName + " API")
                        .description(appName + " 서비스 API 문서")
                        .version("v0.0.0")
                );
    }

    /**
     * Swagger UI에서 API 그룹 지정
     * Swagger 드롭다운
     */
    @Bean
    public GroupedOpenApi apiGroup(@Value("${spring.application.name}") String appName) {
        return GroupedOpenApi.builder()
                .group(appName)
                .pathsToMatch("/**")
                .build();
    }
}
