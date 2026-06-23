package com.condoplus.condominio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * Ponto de entrada do condominio-service.
 *
 * <p>Anotações importantes:
 * <ul>
 *   <li>{@code @EnableDiscoveryClient} — registra no Eureka com o nome "condominio-service"</li>
 *   <li>{@code @EnableJdbcAuditing} — habilita @CreatedDate e @LastModifiedDate nas entidades
 *       (Spring Data JDBC, não JPA)</li>
 *   <li>{@code @ConfigurationPropertiesScan} — carrega classes @ConfigurationProperties
 *       do pacote config</li>
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJdbcAuditing
@ConfigurationPropertiesScan("com.condoplus.condominio.config")
public class CondominioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CondominioServiceApplication.class, args);
    }
}
