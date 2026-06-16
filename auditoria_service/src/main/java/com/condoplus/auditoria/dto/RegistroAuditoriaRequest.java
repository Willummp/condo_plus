package com.condoplus.auditoria.dto;

import com.condoplus.auditoria.domain.EntidadeAfetada;
import com.condoplus.auditoria.domain.PessoaIniciadora;
import com.condoplus.auditoria.domain.RegistroAuditoria;
import com.condoplus.auditoria.domain.TipoEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * DTO de entrada do POST /auditoria/registros.
 * Controla quais campos o cliente pode enviar — id e dataInsercao
 * sao do servidor e ficam de fora de proposito.
 */
@Getter
@Setter
public class RegistroAuditoriaRequest {

    @NotBlank(message = "eventId e obrigatorio para garantir idempotencia")
    private String eventId;

    private String correlationId;

    @NotNull(message = "timestamp do evento de origem e obrigatorio")
    private Instant timestamp;

    @NotNull(message = "tipoEvento e obrigatorio")
    private TipoEvento tipoEvento;

    @NotBlank(message = "servicoOrigem e obrigatorio")
    private String servicoOrigem;

    private PessoaIniciadora pessoaIniciadora;
    private EntidadeAfetada entidadeAfetada;
    private Map<String, Object> payload;
    private Map<String, String> metadados;

    /**
     * Converte o DTO no documento de dominio. id e dataInsercao NAO sao
     * setados: id e gerado pelo Mongo; dataInsercao pelo @CreatedDate.
     * Isso impede o cliente de forjar esses campos.
     */
    public RegistroAuditoria toDomain() {
        return RegistroAuditoria.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .timestamp(timestamp)
                .tipoEvento(tipoEvento)
                .servicoOrigem(servicoOrigem)
                .pessoaIniciadora(pessoaIniciadora)
                .entidadeAfetada(entidadeAfetada)
                .payload(payload)
                .metadados(metadados)
                .build();
    }
}