package com.chargesquare.station.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(
        title = "ChargeSquare Station Service API",
        version = "1.0",
        description = "Station, connector, tariff, and authenticated connector-transition endpoints."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "VIEWER or ADMIN JWT for reads; SERVICE or ADMIN JWT for occupy/release.")
public class OpenApiConfig {
}
