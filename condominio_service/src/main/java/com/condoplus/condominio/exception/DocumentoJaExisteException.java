package com.condoplus.condominio.exception;

public class DocumentoJaExisteException extends RuntimeException {
    public DocumentoJaExisteException(String documento) {
        super("Documento já cadastrado: " + documento);
    }
}
