package com.condoplus.auditoria;

import com.condoplus.auditoria.messaging.EventoConsumer;
import com.condoplus.auditoria.repository.RegistroAuditoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureDataMongo
class AuditoriaKafkaIntegracaoTest {

    @Autowired
    EventoConsumer consumer;

    @Autowired
    RegistroAuditoriaRepository repository;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    private String eventoMultaJson() throws Exception {
        Map<String, Object> envelope = Map.of(
                "eventId", "it-multa-1",
                "eventType", "MultaAplicada",
                "timestamp", "2026-06-16T20:30:00",
                "correlationId", "it-corr-1",
                "originService", "condominio-service",
                "payload", Map.of("id", "multa-it-1")
        );
        return objectMapper.writeValueAsString(envelope);
    }

    @Test
    void deveArquivarEventoConsumido() throws Exception {
        consumer.consumirMultaAplicada(eventoMultaJson());

        assertThat(repository.findByEventId("it-multa-1")).isPresent();
        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    void deveSerIdempotenteParaEventoDuplicado() throws Exception {
        consumer.consumirMultaAplicada(eventoMultaJson());
        consumer.consumirMultaAplicada(eventoMultaJson());

        assertThat(repository.count()).isEqualTo(1L);
    }
}
