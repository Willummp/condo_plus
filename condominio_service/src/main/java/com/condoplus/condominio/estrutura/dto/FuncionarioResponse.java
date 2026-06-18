package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.CargoFuncionario;
import com.condoplus.condominio.estrutura.domain.Funcionario;

import java.time.LocalDate;
import java.util.UUID;

public record FuncionarioResponse(
    UUID id,
    UUID pessoaId,
    CargoFuncionario cargo,
    LocalDate dataAdmissao,
    LocalDate dataDesligamento,
    boolean ativo
) {
    public static FuncionarioResponse fromEntity(Funcionario f) {
        return new FuncionarioResponse(
            f.getId(),
            f.getPessoaId() != null ? f.getPessoaId().getId() : null,
            f.getCargo(),
            f.getDataAdmissao(),
            f.getDataDesligamento(),
            f.isAtivo()
        );
    }
}
