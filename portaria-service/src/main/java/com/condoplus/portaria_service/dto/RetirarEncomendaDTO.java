package com.condoplus.portaria_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RetirarEncomendaDTO(

        @NotNull(message = "encomendaId é obrigatório")
        UUID encomendaId,

        @NotNull(message = "retiradoPorPessoaId é obrigatório")
        UUID retiradoPorPessoaId,

        @NotNull(message = "porteiroId é obrigatório")
        UUID porteiroId

) {}