package com.booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ADMIN_SECURITY_SCHEME = "adminBearer";

    @Bean
    public OpenAPI bookingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Appointment Booking API")
                        .version("v1")
                        .description("""
                                Public endpoints (/api/companies/**) need no authentication.
                                Admin endpoints (/api/admin/**) require a bearer session token obtained \
                                through the magic-link flow (/api/admin/auth/request-link, then /api/admin/auth/verify).
                                Errors are returned as RFC 7807 problem details."""))
                .components(new Components().addSecuritySchemes(ADMIN_SECURITY_SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")));
    }
}
