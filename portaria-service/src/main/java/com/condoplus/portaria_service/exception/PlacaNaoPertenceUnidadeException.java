package com.condoplus.portaria_service.exception;

import java.util.UUID;

public class PlacaNaoPertenceUnidadeException extends RuntimeException {
    public PlacaNaoPertenceUnidadeException(String placa, UUID unidadeId) {
        super("Placa " + placa + " não pertence à unidade " + unidadeId);
    }
}
