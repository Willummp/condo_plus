package com.condoplus.condominio.convivencia.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record NovaAreaComumRequest(
    @NotBlank(message = "O nome da área comum é obrigatório")
    @Size(max = 100, message = "O nome da área comum não pode exceder 100 caracteres")
    String nome,

    @NotNull(message = "A capacidade máxima é obrigatória")
    @Min(value = 1, message = "A capacidade máxima deve ser de pelo menos 1 pessoa")
    Integer capacidadeMaxima,

    @DecimalMin(value = "0.00", message = "O valor da reserva não pode ser negativo")
    BigDecimal valorReserva,

    String regras
) {}
