package com.condoplus.observability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration central do starter.
 *
 * Atualmente nao registra nenhum @Bean: toda a configuracao da Fase 1 acontece
 * via dependencias transitivas (Micrometer + OTel + Prometheus) + o YAML
 * {@code application-observability.yml} que cada servico importa explicitamente.
 *
 * Mantemos esta classe como ponto de entrada nominal por dois motivos:
 *  1. Facilitar a evolucao: a Fase 3 (OTel Collector) e a Fase 4 (sampling adaptativo,
 *     custom ObservationHandlers, instrumentacao do Resilience4j) registrarao @Beans aqui.
 *  2. Permitir o opt-out via {@code condoplus.observability.enabled=false} sem que
 *     o desenvolvedor precise excluir auto-configs do Spring Boot manualmente.
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "condoplus.observability",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(CondoplusObservabilityProperties.class)
public class ObservabilityAutoConfiguration {
}