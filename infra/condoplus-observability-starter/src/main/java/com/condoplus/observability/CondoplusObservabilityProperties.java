package com.condoplus.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de customizacao do starter de observabilidade do Condo+.
 *
 * Prefixo: {@code condoplus.observability}.
 *
 * Hoje so existe a flag {@code enabled} para o caso de um servico
 * (ex.: teste de carga local) precisar desligar tudo sem mexer em dependencias.
 * Pontos de extensao futuros: nome do servico no Zipkin, lista de tags
 * globais de metricas, sampling adaptativo, etc.
 */
@ConfigurationProperties(prefix = "condoplus.observability")
public class CondoplusObservabilityProperties {

    /**
     * Habilita o starter por completo. Quando {@code false}, a auto-configuration
     * nao registra nada e o servico se comporta como se o starter nao existisse
     * (Actuator e tracing seguem funcionando se houver outras configuracoes do Spring Boot).
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}