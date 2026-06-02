package com.condoplus.condominio.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuração dos clientes REST.
 *
 * <p>PROBLEMA: se você marcar @LoadBalanced em um RestClient sem critério,
 * TODAS as URLs viram alvos do Eureka. Tentar chamar uma URL externa causa erro
 * porque o balanceador tenta resolver o nome no Eureka primeiro.
 *
 * <p>SOLUÇÃO: dois beans separados por @Qualifier:
 * <ul>
 *   <li>{@code balancedRestClient} — para chamadas a outros microsserviços
 *       (usa lb://nome-do-servico, resolvido pelo Eureka)</li>
 *   <li>{@code rawRestClient} — para chamadas a URLs externas ou diretas</li>
 * </ul>
 *
 * <p>Para injetar: {@code @Qualifier("balancedRestClient") RestClient.Builder builder}
 */
@Configuration
public class RestClientConfig {

    /**
     * RestClient balanceado: resolve nomes lógicos no Eureka.
     * Use para chamadas entre microsserviços via lb://nome-do-servico.
     */
    @Bean("balancedRestClient")
    @LoadBalanced
    RestClient.Builder balancedRestClientBuilder() {
        return RestClient.builder();
    }

    /**
     * RestClient comum: sem balanceamento.
     * Use para chamadas a URLs externas ou diretas.
     */
    @Bean("rawRestClient")
    RestClient.Builder rawRestClientBuilder() {
        return RestClient.builder();
    }
}
