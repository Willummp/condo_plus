package com.condoplus.condominio.convivencia.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record NovaReservaRequest(
    @NotNull UUID areaComumId,
    @NotNull @Future LocalDate dataReserva,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFim
) {}
