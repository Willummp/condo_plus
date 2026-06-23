package com.condoplus.condominio.convivencia.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

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

    
    @Column("valor_reserva")
    private BigDecimal valorReserva;

    @Column("regras")
    private String regras;

    @Column("ativa")
    private boolean ativa;
}
