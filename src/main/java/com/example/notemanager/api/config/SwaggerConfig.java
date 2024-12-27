package com.example.notemanager.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("Bearer Authorisation"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authorisation",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .bearerFormat("JWT")
                                        .scheme("bearer")))
                .info(new Info()
                        .title("Pawprints On Pages: The Cat Notes Service")
                        .description("A safe and thoughtful space to capture, update, and manage the special moments and adventures in your cat's life.")
                        .version("1.0")
                );

        openAPI.path("/api/v1/signup", new PathItem()
                        .post(new Operation().summary("Sign up").addTagsItem("Authentication controller").security(List.of())))
                .path("/api/v1/login", new PathItem()
                        .post(new Operation().summary("Login").addTagsItem("Authentication controller").security(List.of())));

        return openAPI;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "swagger-ui.html", "/swagger-resources/**", "/api-docs/**", "swagger-ui/**"
        );
    }
}