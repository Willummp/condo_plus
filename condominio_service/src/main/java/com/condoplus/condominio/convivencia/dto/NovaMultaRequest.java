package com.condoplus.condominio.convivencia.dto;

import com.condoplus.condominio.convivencia.domain.CategoriaMulta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) de entrada que representa uma solicitação de aplicação de nova Multa.
 * 
 * <p>Anotações de validação dos campos:
 * <ul>
 *   <li>{@code @NotNull} — Torna obrigatório o preenchimento de campos essenciais (como unidadeId, categoria, vencimento e valor).</li>
 *   <li>{@code @DecimalMin} — Garante que o valor da multa inserido seja maior que zero (mínimo de R$ 0.01).</li>
 *   <li>{@code @NotBlank} — Exige que o motivo da multa seja preenchido e não contenha apenas espaços vazios.</li>
 * </ul>
 */
public record NovaMultaRequest(
    @NotNull(message = "A unidade é obrigatória")
    UUID unidadeId,

    @NotNull(message = "O valor da multa é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor da multa deve ser maior que zero")
    BigDecimal valor,

    @NotBlank(message = "O motivo da multa é obrigatório")
    String motivo,

    @NotNull(message = "A categoria da multa é obrigatória")
    CategoriaMulta categoria,

    String anexoEvidenciaUrl,

    @NotNull(message = "A data de vencimento da multa é obrigatória")
    LocalDate dataVencimento
) {}
