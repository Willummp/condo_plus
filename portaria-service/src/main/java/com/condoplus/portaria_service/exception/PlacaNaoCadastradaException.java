package com.condoplus.portaria_service.exception;

public class PlacaNaoCadastradaException extends RuntimeException {
    public PlacaNaoCadastradaException(String placa) {
        super("Placa não cadastrada no sistema: " + placa);
    }
}
