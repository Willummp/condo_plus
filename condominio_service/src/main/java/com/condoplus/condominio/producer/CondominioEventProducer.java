package com.condoplus.condominio.producer;

import com.condoplus.condominio.event.ComunicadoPublicadoEvent;
import com.condoplus.condominio.event.EventEnvelope;
import com.condoplus.condominio.event.MultaAplicadaEvent;
import com.condoplus.condominio.event.ReservaConfirmadaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Produtor de eventos de domínio no Apache Kafka (tp2).
 * Envia envelopes padronizados contendo metadados (incluindo Correlation ID para logs estruturados).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CondominioEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publicarComunicado(ComunicadoPublicadoEvent event) {
        log.info("Publicando evento ComunicadoPublicado no Kafka [tp2]: id={}", event.comunicadoId());
        String correlationId = event.correlationId() != null ? event.correlationId() : MDC.get("correlationId");
        EventEnvelope<ComunicadoPublicadoEvent> envelope = EventEnvelope.wrap(
                "ComunicadoPublicado",
                correlationId,
                event
        );
        kafkaTemplate.send("comunicados.publicados", event.comunicadoId().toString(), envelope);
    }

    public void publicarMulta(MultaAplicadaEvent event) {
        log.info("Publicando evento MultaAplicada no Kafka [tp2]: id={}", event.id());
        String correlationId = event.correlationId() != null ? event.correlationId() : MDC.get("correlationId");
        EventEnvelope<MultaAplicadaEvent> envelope = EventEnvelope.wrap(
                "MultaAplicada",
                correlationId,
                event
        );
        kafkaTemplate.send("multas.aplicadas", event.id().toString(), envelope);
    }

    public void publicarReserva(ReservaConfirmadaEvent event) {
        log.info("Publicando evento ReservaConfirmada no Kafka [tp2]: id={}", event.id());
        String correlationId = event.correlationId() != null ? event.correlationId() : MDC.get("correlationId");
        EventEnvelope<ReservaConfirmadaEvent> envelope = EventEnvelope.wrap(
                "ReservaConfirmada",
                correlationId,
                event
        );
        kafkaTemplate.send("reservas.confirmadas", event.id().toString(), envelope);
    }
}
