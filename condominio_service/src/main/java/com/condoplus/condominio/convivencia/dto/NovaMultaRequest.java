package com.condoplus.condominio.convivencia.dto;

import com.condoplus.condominio.convivencia.domain.CategoriaMulta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
