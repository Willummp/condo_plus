package com.condoplus.condominio.estrutura.domain;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Value Object (Record) que mapeia a relação entre uma vinculação e seus escopos associados.
 * 
 * <p>Por que utilizar o formato {@code record} do Java?
 * <ul>
 *   <li>Representa perfeitamente o conceito de Value Object do DDD, pois não possui identidade própria (ID) e
 *   é inerentemente imutável, fornecendo automaticamente implementações robustas de {@code equals()}, {@code hashCode()} e {@code toString()}.</li>
 *   <li>O Spring Data JDBC possui suporte nativo completo para records a partir do Spring Framework 6.x / Spring Boot 3.x.</li>
 * </ul>
 * 
 * <p>Anotações e mapeamentos aplicados:
 * <ul>
 *   <li>{@code @Table("vinculacao_escopo", schema = "condominio")} — Mapeia este valor para a tabela relacional de ligação "vinculacao_escopo" no PostgreSQL.</li>
 * </ul>
 * 
 * @param escopo O enum do privilégio associado à vinculação (ex: SOCIAL, LEGAL, FINANCEIRO).
 */
@Table(schema = "condominio", value = "vinculacao_escopo")
public record VinculacaoEscopo(@Column("escopo") Escopo escopo) {
}
