package com.condoplus.condominio.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MultaAplicadaEvent(
        UUID id,
        UUID unidadeId,
        UUID autorId,
        String descricao,
        BigDecimal valor,
        LocalDateTime dataAplicacao,
        String correlationId
) {
}
