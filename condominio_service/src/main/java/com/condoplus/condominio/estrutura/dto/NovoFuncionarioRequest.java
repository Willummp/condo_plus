package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.CargoFuncionario;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO (Record) de entrada contendo os parâmetros necessários para admitir um funcionário operacional.
 * 
 * @param pessoaId O ID exclusivo (UUID) da pessoa física correspondente. Não pode ser nulo.
 * @param cargo O cargo ocupacional a ser exercido (ex: PORTEIRO, ZELADOR). Não pode ser nulo.
 * @param dataAdmissao A data oficial de início das atividades trabalhistas. Não pode ser nula.
 */
public record NovoFuncionarioRequest(
    @NotNull(message = "A pessoa física vinculada é obrigatória")
    UUID pessoaId,

    @NotNull(message = "O cargo do funcionário é obrigatório")
    CargoFuncionario cargo,

    @NotNull(message = "A data de admissão é obrigatória")
    LocalDate dataAdmissao
) {}
