package com.condoplus.iam.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "condoplus.jwt")
@Validated
public record JwtProperties(

        @NotBlank
        @Size(
                min = 32,
                message = "JWT secret deve ter no mínimo 32 caracteres (256 bits)"
        )
        String secret,

        @Min(60)
        long expirationSeconds,

        @Min(0)
        long refreshGracePeriodSeconds,

        @NotBlank
        String issuer

) {
}