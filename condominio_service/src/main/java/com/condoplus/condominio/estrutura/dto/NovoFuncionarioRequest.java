package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.CargoFuncionario;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record NovoFuncionarioRequest(
    @NotNull(message = "A pessoa física vinculada é obrigatória")
    UUID pessoaId,

    @NotNull(message = "O cargo do funcionário é obrigatório")
    CargoFuncionario cargo,

    @NotNull(message = "A data de admissão é obrigatória")
    LocalDate dataAdmissao
) {}
