package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.TipoUnidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO (Record) de entrada contendo os parâmetros necessários para cadastrar uma nova unidade residencial.
 * 
 * @param numero O número identificador da unidade habitacional (ex: "101", "A-12"). 
 *               Não pode estar em branco e possui limite máximo de 20 caracteres.
 * @param bloco O identificador opcional do bloco/torre residencial. 
 *              Possui limite máximo de 20 caracteres se informado.
 * @param tipo O tipo da habitação (APARTAMENTO/CASA). Não pode ser nulo.
 */
public record NovaUnidadeRequest(
    @NotBlank @Size(max = 20) String numero,
    @Size(max = 20) String bloco,
    @NotNull TipoUnidade tipo
) {}
