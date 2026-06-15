package com.condoplus.portaria_service.dto;

import com.condoplus.portaria_service.model.enums.TipoEncomenda;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CriarEncomendaDTO(

        @NotNull(message = "unidadeId é obrigatório")
        UUID unidadeId,

        @NotNull(message = "tipo é obrigatório")
        TipoEncomenda tipo,

        @Size(max = 500, message = "descrição deve ter no máximo 500 caracteres")
        String descricao,

        @Size(max = 100, message = "código de rastreio deve ter no máximo 100 caracteres")
        String codigoRastreio,

        @NotNull(message = "porteiroRecebedorId é obrigatório")
        UUID porteiroRecebedorId

) {}