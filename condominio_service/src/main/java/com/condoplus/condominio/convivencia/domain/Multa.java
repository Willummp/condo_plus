package com.condoplus.condominio.convivencia.domain;

import com.condoplus.condominio.estrutura.domain.Pessoa;
import com.condoplus.condominio.estrutura.domain.Unidade;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(schema = "condominio", value = "multa")
@Getter
@Setter
@NoArgsConstructor
public class Multa {

    @Id
    private UUID id;

    @Column("unidade_id")
    private AggregateReference<Unidade, UUID> unidadeId;

    @Column("valor")
    private BigDecimal valor;

    @Column("motivo")
    private String motivo;

    @Column("categoria")
    private CategoriaMulta categoria;

    @Column("anexo_evidencia_url")
    private String anexoEvidenciaUrl;

    @Column("data_aplicacao")
    private LocalDateTime dataAplicacao;

    @Column("data_vencimento")
    private LocalDate dataVencimento;

    @Column("status")
    private StatusMulta status;

    @Column("aplicada_por_id")
    private AggregateReference<Pessoa, UUID> aplicadaPorId;
}
