package com.epam.edp.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "Bearer";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Travel Agency API")
                        .version("2.0.0")
                        .description("Sprint 2 — Auth, Tours, Bookings, and Tour Booking Lifecycle Management. "
                                + "Includes JWT-based authentication, tour browsing, booking lifecycle "
                                + "(BOOKED → CONFIRMED → STARTED → FINISHED / CANCELED), document upload "
                                + "& verification, and travel-agent dedicated endpoints.")
                        .contact(new Contact()
                                .name("Travel Agency Team")
                                .email("support@travelagency.com")))
                .servers(List.of(
                        new Server().url("/").description("Current server")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT access token here")))
                .tags(List.of(
                        new Tag().name("Auth").description("Sign-up, sign-in, token refresh and logout"),
                        new Tag().name("Tours").description("Destination search, tour listing, details and reviews"),
                        new Tag().name("Bookings").description("Tour booking lifecycle: create, confirm, cancel, "
                                + "upload/verify documents, record customer approval"),
                        new Tag().name("Travel Agent").description("Travel-agent portal: list and manage assigned bookings")
                ));
    }
}

