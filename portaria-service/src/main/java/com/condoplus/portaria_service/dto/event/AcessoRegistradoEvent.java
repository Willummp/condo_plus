package com.condoplus.portaria_service.dto.event;

import java.time.Instant;

public record AcessoRegistradoEvent(
        String eventId,
        Instant timestamp,
        String correlationId,
        String acessoId,
        String tipoPessoa,
        String pessoaId,
        String unidadeId,
        String tipoMovimento,
        String veiculoPlaca
) {}
