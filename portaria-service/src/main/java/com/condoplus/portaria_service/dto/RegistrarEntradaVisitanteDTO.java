package com.condoplus.portaria_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegistrarEntradaVisitanteDTO(

        @NotBlank(message = "documento é obrigatório")
        @Size(max = 20, message = "documento deve ter no máximo 20 caracteres")
        @Pattern(regexp = "^[0-9A-Za-z.\\-/]+$", message = "documento com formato inválido")
        String documento,

        @NotNull(message = "porteiroId é obrigatório")
        UUID porteiroId

) {}