package com.condoplus.condominio.exception;

public class AreaComumIndisponivelException extends RuntimeException {
    public AreaComumIndisponivelException(String nome) {
        super("Área comum '" + nome + "' está desativada");
    }
}
