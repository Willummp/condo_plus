package com.condoplus.condominio.integration;

import com.condoplus.condominio.event.ComunicadoPublicadoEvent;
import com.condoplus.condominio.event.CredencialCriadaEvent;
import com.condoplus.condominio.event.MultaAplicadaEvent;
import com.condoplus.condominio.event.ReservaConfirmadaEvent;
import com.condoplus.condominio.estrutura.domain.Pessoa;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import com.condoplus.condominio.producer.CondominioEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
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
import java.util.stream.StreamSupport;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Deve publicar evento de ComunicadoPublicado com sucesso no Kafka de teste [tp2]")
    void devePublicarEventoComunicado() {
        ComunicadoPublicadoEvent event = new ComunicadoPublicadoEvent(
                UUID.randomUUID(),
                "Aviso Teste",
                UUID.randomUUID(),
                "GERAL",
                LocalDateTime.now(),
                "corr-123"
        );

        assertDoesNotThrow(() -> eventProducer.publicarComunicado(event));
    }

    @Test
    @DisplayName("Deve publicar evento de MultaAplicada com sucesso no Kafka de teste [tp2]")
    void devePublicarEventoMulta() {
        MultaAplicadaEvent event = new MultaAplicadaEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Barulho",
                new java.math.BigDecimal("150.00"),
                LocalDateTime.now(),
                "corr-456"
        );

        assertDoesNotThrow(() -> eventProducer.publicarMulta(event));
    }

    @Test
    @DisplayName("Deve publicar evento de ReservaConfirmada com sucesso no Kafka de teste [tp2]")
    void devePublicarEventoReserva() {
        ReservaConfirmadaEvent event = new ReservaConfirmadaEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                java.time.LocalDate.now(),
                java.time.LocalTime.of(10, 0),
                java.time.LocalTime.of(12, 0),
                "corr-789"
        );

        assertDoesNotThrow(() -> eventProducer.publicarReserva(event));
    }

    @Test
    @DisplayName("Deve consumir evento de CredencialCriada e persistir Pessoa de forma idempotente [tp2]")
    void deveConsumirCredencialCriadaEPersistirPessoa() throws Exception {
        UUID credencialId = UUID.randomUUID();
        String documento = "12345678909";
        CredencialCriadaEvent event = new CredencialCriadaEvent(
                credencialId,
                "user@test.com",
                documento,
                "User Test",
                "11988887777",
                "MORADOR",
                "corr-abc"
        );

        kafkaTemplate.send("credenciais.criadas", credencialId.toString(), event).get(5, SECONDS);

        boolean salva = false;
        for (int i = 0; i < 50; i++) {
            if (pessoaRepository.existsByDocumento(documento)) {
                salva = true;
                break;
            }
            Thread.sleep(100);
        }

        assertThat(salva).isTrue();

        Pessoa pessoa = pessoaRepository.findByDocumento(documento).orElseThrow();
        assertThat(pessoa.getCredencialId()).isEqualTo(credencialId);
        assertThat(pessoa.getNomeCompleto()).isEqualTo("User Test");

        assertDoesNotThrow(() -> {
            kafkaTemplate.send("credenciais.criadas", credencialId.toString(), event).get(5, SECONDS);
        });

        Thread.sleep(500);
        long count = StreamSupport.stream(pessoaRepository.findAll().spliterator(), false)
                .filter(p -> p.getDocumento().equals(documento))
                .count();
        assertThat(count).isEqualTo(1);
    }
}
