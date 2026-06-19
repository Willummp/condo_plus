package com.condoplus.portaria_service.exception;

import java.util.UUID;

public class VisitanteNaoEncontradoException extends RuntimeException {
    public VisitanteNaoEncontradoException(UUID id) {
        super("Visitante não encontrado: " + id);
    }
}
