package com.condoplus.condominio.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ComunicadoPublicadoEvent(
        UUID comunicadoId,
        String titulo,
        UUID autorId,
        String publicoAlvo,
        LocalDateTime dataPublicacao,
        String correlationId
) {
}
