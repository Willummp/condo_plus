package com.condoplus.condominio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJdbcAuditing
@ConfigurationPropertiesScan("com.condoplus.condominio.config")
public class CondominioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CondominioServiceApplication.class, args);
    }
}
