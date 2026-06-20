package com.condoplus.auditoria;

import com.condoplus.auditoria.messaging.EventEnvelope;
import com.condoplus.auditoria.messaging.EventoConsumer;
import com.condoplus.auditoria.repository.RegistroAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integracao do fluxo de arquivamento do TP2: consumer -> mapper ->
 * idempotencia -> MongoDB. Usa MongoDB embarcado em memoria (flapdoodle, via
 * @AutoConfigureDataMongo); nao depende de Docker.
 *
 * Decisao: em vez de subir um broker Kafka (@EmbeddedKafka), que no Windows e
 * instavel na limpeza de arquivos temporarios, o teste chama o EventoConsumer
 * diretamente com um EventEnvelope. Exercita exatamente a mesma logica de
 * negocio (mapeamento + persistencia idempotente), de forma deterministica e
 * sem dependencia de broker/SO. A serializacao Kafka em si ja foi validada ao
 * vivo (console-producer) durante o desenvolvimento.
 */
@SpringBootTest
@AutoConfigureDataMongo
class AuditoriaKafkaIntegracaoTest {

    @Autowired
    EventoConsumer consumer;

    @Autowired
    RegistroAuditoriaRepository repository;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    private EventEnvelope eventoMulta() {
        return new EventEnvelope(
                "it-multa-1",
                "MultaAplicada",
                Instant.parse("2026-06-16T20:30:00Z"),
                "it-corr-1",
                "condominio-service",
                Map.of("id", "multa-it-1")
        );
    }

    @Test
    void deveArquivarEventoConsumido() {
        consumer.consumirMultaAplicada(eventoMulta());

        assertThat(repository.findByEventId("it-multa-1")).isPresent();
        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    void deveSerIdempotenteParaEventoDuplicado() {
        // Mesmo eventId processado duas vezes: o indice unique + o catch de
        // DuplicateKeyException no AuditoriaService devem impedir a duplicata.
        consumer.consumirMultaAplicada(eventoMulta());
        consumer.consumirMultaAplicada(eventoMulta());

        assertThat(repository.count()).isEqualTo(1L);
    }
}