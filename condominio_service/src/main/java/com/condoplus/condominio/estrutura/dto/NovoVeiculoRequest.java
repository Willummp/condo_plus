package com.condoplus.condominio.estrutura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO (Record) de entrada contendo os parâmetros necessários para cadastrar um novo veículo e associá-lo a uma unidade.
 * 
 * @param placa A placa regulamentar do veículo. Não pode estar em branco e deve respeitar a expressão regular de formato (ex: AAA9B99 ou AAA-9999).
 * @param modelo O modelo e fabricante do veículo (ex: "Hyundai HB20"). Não pode estar em branco e possui limite máximo de 100 caracteres.
 * @param cor A cor predominante do veículo (ex: "Preto"). Não pode estar em branco e possui limite máximo de 50 caracteres.
 * @param unidadeId O ID exclusivo (UUID) da unidade residencial associada. Não pode ser nulo.
 */
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
