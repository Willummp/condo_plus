package com.condoplus.portaria_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * RestClient balanceado pelo Eureka — use para chamar serviços internos
     * como condominio-service. Nunca use para URLs externas.
     */
    @Bean("balancedRestClient")
    @LoadBalanced
    RestClient.Builder balancedRestClientBuilder() {
        return RestClient.builder();
    }

    /**
     * RestClient sem balanceamento — use para chamadas a URLs externas fixas.
     */
    @Bean("rawRestClient")
    RestClient.Builder rawRestClientBuilder() {
        return RestClient.builder();
    }
}
