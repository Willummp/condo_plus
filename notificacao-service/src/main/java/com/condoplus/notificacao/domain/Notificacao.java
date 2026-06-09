package com.condoplus.notificacao.domain;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(schema = "notificacao", value = "notificacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Notificacao {
    @Id
    private UUID id;

    @Column("destinatario_pessoa_id")
    private UUID destinatarioPessoaId;

    @Column("tipo_evento")
    private TipoEvento tipoEvento;

    @Column("evento_origem_id")
    private String eventoOrigemId;

    @Column("canal")
    private Canal canal;

    @Column("titulo")
    private String titulo;

    @Column("corpo")
    private String corpo;

    @Column("status")
    private StatusNotificacao status;

    @Column("tentativas")
    private int tentativas;

    @Column("ultimo_erro")
    private String ultimoErro;

    @CreatedDate
    @Column("criada_em")
    private LocalDateTime criadaEm;

    @Column("enviada_em")
    private LocalDateTime enviadaEm;
}
