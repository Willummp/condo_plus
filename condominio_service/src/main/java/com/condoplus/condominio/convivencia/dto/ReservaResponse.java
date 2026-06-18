package com.condoplus.condominio.convivencia.dto;

import com.condoplus.condominio.convivencia.domain.Reserva;
import com.condoplus.condominio.convivencia.domain.StatusReserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ReservaResponse(
    UUID id,
    UUID areaComumId,
    String areaComumNome,
    UUID moradorId,
    LocalDate dataReserva,
    LocalTime horaInicio,
    LocalTime horaFim,
    StatusReserva status,
    LocalDateTime criadaEm
) {
    public static ReservaResponse fromEntity(Reserva r, String areaComumNome) {
        return new ReservaResponse(
            r.getId(),
            r.getAreaComumId().getId(),
            areaComumNome,
            r.getMoradorId().getId(),
            r.getDataReserva(),
            r.getHoraInicio(),
            r.getHoraFim(),
            r.getStatus(),
            r.getCriadaEm()
        );
    }
}
