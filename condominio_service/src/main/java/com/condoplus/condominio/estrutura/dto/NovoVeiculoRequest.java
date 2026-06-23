package com.condoplus.condominio.estrutura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record NovoVeiculoRequest(
    @NotBlank(message = "A placa é obrigatória")
    @Pattern(regexp = "^[a-zA-Z0-9-]{7,8}$", message = "Placa deve estar em formato válido (ex: AAA9B99 ou AAA-9999)")
    String placa,

    @NotBlank(message = "O modelo é obrigatório")
    @Size(max = 100, message = "O modelo não pode exceder 100 caracteres")
    String modelo,

    @NotBlank(message = "A cor é obrigatória")
    @Size(max = 50, message = "A cor não pode exceder 50 caracteres")
    String cor,

    @NotNull(message = "A unidade vinculada é obrigatória")
    UUID unidadeId
) {}
