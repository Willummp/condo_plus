package com.condoplus.condominio.convivencia.domain;

import com.condoplus.condominio.estrutura.domain.Pessoa;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(schema = "condominio", value = "comunicado")
@Getter
@Setter
@NoArgsConstructor
public class Comunicado {

    @Id
    private UUID id;

    @Column("titulo")
    private String titulo;

    @Column("mensagem")
    private String mensagem;

    @Column("data_publicacao")
    private LocalDateTime dataPublicacao;

    @Column("autor_id")
    private AggregateReference<Pessoa, UUID> autorId;

    @Column("publico_alvo")
    private PublicoAlvo publicoAlvo;

    
    @Column("bloco_alvo")
    private String blocoAlvo;
}
