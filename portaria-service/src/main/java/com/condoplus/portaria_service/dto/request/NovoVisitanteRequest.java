package com.condoplus.portaria_service.dto.request;

import com.condoplus.portaria_service.model.enums.TipoVisitante;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record NovoVisitanteRequest(
        @NotBlank @Size(max = 200) String nome,
        @Size(max = 20) String documento,
        @Size(max = 20) String telefone,
        @NotNull TipoVisitante tipo,
        @NotNull UUID unidadeId,
        @NotNull @Future LocalDateTime validadeInicio,
        @NotNull @Future LocalDateTime validadeFim
) {}
