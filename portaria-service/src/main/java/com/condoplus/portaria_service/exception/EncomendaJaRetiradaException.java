package com.condoplus.portaria_service.exception;

import com.condoplus.portaria_service.model.enums.StatusEncomenda;

import java.util.UUID;

public class EncomendaJaRetiradaException extends RuntimeException {
    public EncomendaJaRetiradaException(UUID id, StatusEncomenda status) {
        super("Encomenda " + id + " já está em status " + status);
    }
}
