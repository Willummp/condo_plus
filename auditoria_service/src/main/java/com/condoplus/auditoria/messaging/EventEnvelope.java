package com.condoplus.auditoria.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;

/**
 * Contrato de LEITURA do envelope de evento, mantido no proprio auditoria.
 *
 * Espelha o EventEnvelope que os produtores (ex: condominio-service) publicam,
 * mas de forma deliberadamente independente: o auditoria nao depende do modelo
 * dos outros servicos. Le os metadados que entende (eventId, eventType,
 * correlationId, originService, timestamp) e trata o payload como um mapa
 * generico — qualquer estrutura cabe, sem acoplar o auditoria a cada formato.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true): se um produtor adicionar um
 * campo novo no envelope, o auditoria nao quebra — simplesmente ignora.
 * Essa tolerancia e proposital: auditoria consome eventos de todos e nao pode
 * ser fragil a mudancas alheias.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        String eventId,
        String eventType,
        Instant timestamp,
        String correlationId,
        String originService,
        Map<String, Object> payload
) {
}