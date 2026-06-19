package com.condoplus.portaria_service.exception;

import java.util.UUID;

public class VisitanteNaoAtivoException extends RuntimeException {
    public VisitanteNaoAtivoException(UUID id) {
        super("Visitante não está ativo: " + id);
    }
}
