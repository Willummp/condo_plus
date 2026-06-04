package com.condoplus.condominio.exception;

import java.time.LocalDate;

public class ConflitoReservaException extends RuntimeException {
    public ConflitoReservaException(String areaComum, LocalDate data) {
        super("Já existe reserva confirmada para " + areaComum + " em " + data);
    }
}
