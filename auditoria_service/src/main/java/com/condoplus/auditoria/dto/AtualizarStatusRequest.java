package com.condoplus.auditoria.dto;

import com.condoplus.auditoria.domain.StatusAnomalia;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do PATCH de status. @NotNull garante que o cliente informe um
 * status valido (Spring converte a String para o enum; valor invalido -> 400).
 */
public record AtualizarStatusRequest(
        @NotNull(message = "status e obrigatorio") StatusAnomalia status
) {
}