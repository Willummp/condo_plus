package com.condoplus.portaria_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient          // Registra no Eureka
@EnableScheduling               // Habilita o EncomendaExpiracaoJob (@Scheduled)
@ConfigurationPropertiesScan(   // Detecta o PortariaProperties automaticamente
		"com.condoplus.portaria_service.config.properties"
)
public class PortariaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortariaServiceApplication.class, args);
	}
}