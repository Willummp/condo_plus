package com.condoplus.condominio.exception;

import java.util.UUID;

public class PessoaNaoEncontradaException extends RuntimeException {
    public PessoaNaoEncontradaException(String detalhe) {
        super("Pessoa não encontrada: " + detalhe);
    }

    public PessoaNaoEncontradaException(UUID id) {
        super("Pessoa não encontrada: " + id);
    }
}
