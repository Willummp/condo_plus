package com.condoplus.condominio.convivencia.dto;

import com.condoplus.condominio.convivencia.domain.Comunicado;
import com.condoplus.condominio.convivencia.domain.PublicoAlvo;

import java.time.LocalDateTime;
import java.util.UUID;

public record ComunicadoResponse(
    UUID id,
    String titulo,
    String mensagem,
    LocalDateTime dataPublicacao,
    UUID autorId,
    PublicoAlvo publicoAlvo,
    String blocoAlvo
) {
    public static ComunicadoResponse fromEntity(Comunicado c) {
        return new ComunicadoResponse(
            c.getId(),
            c.getTitulo(),
            c.getMensagem(),
            c.getDataPublicacao(),
            c.getAutorId() != null ? c.getAutorId().getId() : null,
            c.getPublicoAlvo(),
            c.getBlocoAlvo()
        );
    }
}
