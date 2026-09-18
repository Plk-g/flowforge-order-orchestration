package com.flowforge.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FlowForge Order API")
                .version("v1")
                .description("REST writes for the order orchestration platform. Query status with GraphQL at /graphql."));
    }
}
