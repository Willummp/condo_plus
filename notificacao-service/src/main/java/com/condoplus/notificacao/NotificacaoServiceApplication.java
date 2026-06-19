package com.condoplus.notificacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan("com.condoplus.notificacao.config.properties")
public class NotificacaoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificacaoServiceApplication.class, args);
    }
}