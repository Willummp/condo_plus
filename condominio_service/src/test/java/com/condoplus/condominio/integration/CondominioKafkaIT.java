package com.condoplus.condominio.integration;

import com.condoplus.condominio.event.ComunicadoPublicadoEvent;
import com.condoplus.condominio.producer.CondominioEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class CondominioKafkaIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("condoplus")
            .withUsername("test_user")
            .withPassword("test_pass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "?currentSchema=condominio");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private CondominioEventProducer eventProducer;

    @Test
    @DisplayName("Deve publicar evento de ComunicadoPublicado com sucesso no Kafka de teste")
    void devePublicarEventoKafka() {
        ComunicadoPublicadoEvent event = new ComunicadoPublicadoEvent(
                UUID.randomUUID(),
                "Aviso Teste",
                UUID.randomUUID(),
                "GERAL",
                LocalDateTime.now(),
                "corr-123"
        );

        // Apenas garantimos que a publicação não lança exceção com o container Kafka rodando
        assertDoesNotThrow(() -> eventProducer.publicarComunicado(event));
    }
}
