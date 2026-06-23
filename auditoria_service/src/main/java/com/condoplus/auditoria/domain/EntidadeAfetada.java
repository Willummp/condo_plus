package com.condoplus.auditoria.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Subdocumento que identifica a entidade afetada pelo evento auditado.
 *
 * Exemplos: tipo="Multa" id="abc-123", tipo="Unidade" id="42-A".
 *
 * Por que 'tipo' como String e nao enum:
 * O dominio evolui — Pessoa, Unidade, Multa, Reserva, Encomenda, Acesso e
 * novos tipos podem aparecer. String livre evita ter que fazer deploy do
 * auditoria-service so para adicionar valor ao enum. Quem produz o evento
 * e quem valida o tipo.
 *
 * Diferente de TipoEvento (que e fechado e conhecido), o universo de
 * entidades auditaveis e aberto e tende a crescer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntidadeAfetada {

    @Field("tipo")
    private String tipo; // "Pessoa", "Unidade", "Multa", "Reserva", "Encomenda", "Acesso", ...

    @Field("id")
    private String id; // String para acomodar UUIDs, ObjectIds e outros formatos
}