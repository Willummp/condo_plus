package com.condoplus.auditoria.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Contrato de LEITURA do envelope de evento, mantido no proprio auditoria.
 *
 * timestamp e Object (em vez de Instant) porque cada producer pode serializar
 * datas de forma diferente (LocalDateTime, Instant, epoch ms). O auditoria nao
 * depende do formato exato — usa Instant.now() no mapper se o campo nao vier.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        String eventId,
        String eventType,
        Object timestamp,
        String correlationId,
        String originService,
        Map<String, Object> payload
) {
}