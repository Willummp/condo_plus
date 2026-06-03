package com.condoplus.condominio.convivencia.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) de entrada que representa uma solicitação de criação de nova Reserva.
 * 
 * <p>Anotações de validação dos campos:
 * <ul>
 *   <li>{@code @NotNull} — Garante que o campo correspondente não seja nulo, sendo obrigatório na requisição.</li>
 *   <li>{@code @Future} — Exige que a data da reserva seja estritamente uma data futura.</li>
 * </ul>
 */
public record NovaReservaRequest(
    @NotNull UUID areaComumId,
    @NotNull @Future LocalDate dataReserva,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFim
) {}
