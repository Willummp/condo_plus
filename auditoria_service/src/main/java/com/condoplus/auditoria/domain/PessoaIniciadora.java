package com.condoplus.auditoria.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.UUID;

/**
 * Subdocumento que identifica quem disparou a acao auditada.
 *
 * Exemplo: SINDICO Joana aplicou uma multa, MORADOR Carlos autorizou um
 * visitante. Pode ser null em eventos de sistema (ex: job interno).
 *
 * Por que armazenar 'nomeCached' alem do id:
 * Auditoria precisa responder "QUEM fez X" mesmo se a Pessoa for renomeada
 * ou apagada do condominio-service no futuro. Cache leve preserva o nome
 * no momento do evento — auditoria nao depende de chamada externa para
 * apresentar log historico. Isso e um exemplo concreto de "denormalizacao
 * deliberada" que NoSQL favorece e que em modelo relacional exigiria join
 * com tabela externa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PessoaIniciadora {

    @Field("id")
    private UUID id; // referencia estavel a Pessoa no condominio-service

    @Field("nomeCached")
    private String nomeCached; // cache do nome no momento do evento

    @Field("roles")
    private String roles; // ex: "MORADOR,SINDICO" — multiplas roles separadas por virgula
}