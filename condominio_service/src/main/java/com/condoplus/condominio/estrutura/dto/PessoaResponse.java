package com.condoplus.condominio.estrutura.dto;

import com.condoplus.condominio.estrutura.domain.Pessoa;

import java.time.LocalDateTime;
import java.util.UUID;

public record PessoaResponse(
    UUID id,
    UUID credencialId,
    String nomeCompleto,
    String documento,
    String telefone,
    String emailContato,
    LocalDateTime criadaEm
) {
    public static PessoaResponse fromEntity(Pessoa p) {
        return new PessoaResponse(
            p.getId(), p.getCredencialId(), p.getNomeCompleto(),
            p.getDocumento(), p.getTelefone(), p.getEmailContato(), p.getCriadaEm()
        );
    }
}
