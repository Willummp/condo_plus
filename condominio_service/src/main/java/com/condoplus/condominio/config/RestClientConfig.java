package com.condoplus.condominio.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RestClientConfig {

    
    @Bean("balancedRestClient")
    @LoadBalanced
    RestClient.Builder balancedRestClientBuilder() {
        return RestClient.builder();
    }

    
    @Bean("rawRestClient")
    RestClient.Builder rawRestClientBuilder() {
        return RestClient.builder();
    }

    
    @Bean("balancedWebClient")
    @LoadBalanced
    WebClient.Builder balancedWebClientBuilder() {
        return WebClient.builder();
    }
}
