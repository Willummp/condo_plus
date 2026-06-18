package com.condoplus.portaria_service.dto.event;

import java.time.Instant;

public record EncomendaRecebidaEvent(
        String eventId,
        Instant timestamp,
        String correlationId,
        String encomendaId,
        String unidadeId,
        String tipo,
        String descricao,
        String codigoRastreio
) {}