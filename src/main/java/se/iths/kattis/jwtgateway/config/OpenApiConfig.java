package se.iths.kattis.jwtgateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurerar dokumentationen (Swagger) för Gatewayens API.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Skapar API-dokumentationen med titel och version, och definierar JWT som Bearer-token.
     *
     * @return OpenAPI-konfigurationen
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JWT Gateway API")
                        .description("Gateway för webbshopens mikrotjänster")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
