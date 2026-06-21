package com.condoplus.portaria_service.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "condoplus.portaria")
@Validated
public record PortariaProperties(
        Encomendas encomendas,
        ExpiracaoJob expiracaoJob
) {
    public record Encomendas(
            @Min(1) int curtoPrazoTtlMinutos,
            @Min(1) int medioPrazoAlertaDias,
            @Min(1) int longoPrazoAlertaDias
    ) {}

    public record ExpiracaoJob(String cron) {}
}
