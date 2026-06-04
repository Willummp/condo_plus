package com.condoplus.condominio.convivencia.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) de entrada que representa uma solicitação de cadastro ou atualização de Área Comum.
 * 
 * <p>Anotações de validação dos campos:
 * <ul>
 *   <li>{@code @NotBlank} — Exige que o nome da área comum seja preenchido e não contenha apenas espaços em branco.</li>
 *   <li>{@code @Size} — Impõe limite máximo de caracteres (100) para o nome da área.</li>
 *   <li>{@code @NotNull} — Torna o preenchimento da capacidade máxima obrigatório.</li>
 *   <li>{@code @Min} — Valida que a capacidade mínima inserida seja de pelo menos 1 pessoa.</li>
 *   <li>{@code @DecimalMin} — Valida que o valor atribuído ao aluguel da reserva não seja negativo (mínimo de R$ 0.00).</li>
 * </ul>
 */
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
