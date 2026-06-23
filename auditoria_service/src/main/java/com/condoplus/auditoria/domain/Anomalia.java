package com.condoplus.auditoria.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Anomalia detectada por uma regra. Segunda colecao do servico (alem de
 * registros_auditoria): aqui mora o resultado da "vigilancia".
 */
@Document(collection = "anomalias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anomalia {

    @Id
    private String id;

    @Indexed
    @Field("tipoRegra")
    private String tipoRegra; // ex: "LOGIN_FALHADO_EXCESSIVO"

    @Indexed
    @Field("severidade")
    private SeveridadeAnomalia severidade;

    @Field("descricao")
    private String descricao;

    @Indexed
    @Field("entidadeRelacionada")
    private String entidadeRelacionada; // ex: a credencial alvo

    @Field("correlationId")
    private String correlationId;

    @Field("eventoGatilhoId")
    private String eventoGatilhoId; // eventId que disparou a deteccao

    @Indexed
    @Field("status")
    private StatusAnomalia status; // ABERTA na criacao

    @CreatedDate
    @Field("detectadaEm")
    private Instant detectadaEm;
}