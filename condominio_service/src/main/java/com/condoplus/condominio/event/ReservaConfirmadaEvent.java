package com.condoplus.condominio.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservaConfirmadaEvent(
        UUID id,
        UUID areaComumId,
        UUID moradorId,
        LocalDate dataReserva,
        LocalTime horaInicio,
        LocalTime horaFim,
        String correlationId
) {
}
