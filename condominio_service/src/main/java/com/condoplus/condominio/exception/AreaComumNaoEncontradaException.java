package com.condoplus.condominio.exception;

import java.util.UUID;

public class AreaComumNaoEncontradaException extends RuntimeException {
    public AreaComumNaoEncontradaException(UUID id) {
        super("Área comum não encontrada: " + id);
    }
}
