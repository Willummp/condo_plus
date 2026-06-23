package com.condoplus.portaria_service.exception;

import java.util.UUID;

public class EncomendaNaoEncontradaException extends RuntimeException {
    public EncomendaNaoEncontradaException(UUID id) {
        super("Encomenda não encontrada: " + id);
    }
}
