package com.condoplus.condominio.exception;

import java.util.UUID;

public class UnidadeNaoEncontradaException extends RuntimeException {
    public UnidadeNaoEncontradaException(UUID id) {
        super("Unidade não encontrada: " + id);
    }
}
