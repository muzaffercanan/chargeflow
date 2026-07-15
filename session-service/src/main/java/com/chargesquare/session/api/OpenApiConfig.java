package com.chargesquare.session.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(
        title = "ChargeSquare Session Service API",
        version = "1.0",
        description = "Login, charging-session lifecycle, receipt, history, and wallet-settlement endpoints."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "VIEWER or ADMIN JWT for reads; ADMIN JWT for session start/stop.")
public class OpenApiConfig {
}
