package com.condoplus.condominio.convivencia.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidade que representa uma Área Comum (ex: piscina, churrasqueira) no contexto de Convivência.
 * 
 * <p>Anotações da classe:
 * <ul>
 *   <li>{@code @Table(schema = "condominio", value = "area_comum")} — Mapeia a classe para a tabela {@code area_comum} dentro do schema {@code condominio} no PostgreSQL.</li>
 *   <li>{@code @Getter} — Gera automaticamente os métodos getters para todos os campos pelo Lombok.</li>
 *   <li>{@code @Setter} — Gera automaticamente os métodos setters para todos os campos pelo Lombok.</li>
 *   <li>{@code @NoArgsConstructor} — Gera automaticamente um construtor sem argumentos exigido pelo framework.</li>
 * </ul>
 */
@Table(schema = "condominio", value = "area_comum")
@Getter
@Setter
@NoArgsConstructor
public class AreaComum {

    @Id
    private UUID id;

    @Column("nome")
    private String nome;

    @Column("capacidade_maxima")
    private Integer capacidadeMaxima;

    /**
     * Valor cobrado para a realização da reserva da área comum. 
     * Pode ser {@code null} se o agendamento da área for gratuito.
     */
    @Column("valor_reserva")
    private BigDecimal valorReserva;

    @Column("regras")
    private String regras;

    @Column("ativa")
    private boolean ativa;
}
