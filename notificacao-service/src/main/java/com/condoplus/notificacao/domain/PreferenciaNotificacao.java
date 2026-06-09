package com.condoplus.notificacao.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(schema = "notificacao", value = "preferencia_notificacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PreferenciaNotificacao {
    @Id
    private UUID id;

    @Column("pessoa_id")
    private UUID pessoaId;

    @Column("tipo_evento")
    private TipoEvento tipoEvento;

    @Column("canal")
    private Canal canal;

    @Column("ativa")
    private boolean ativa;

    @CreatedDate
    @Column("criada_em")
    private LocalDateTime criadaEm;

    @LastModifiedDate
    @Column("atualizada_em")
    private LocalDateTime atualizadaEm;
}
