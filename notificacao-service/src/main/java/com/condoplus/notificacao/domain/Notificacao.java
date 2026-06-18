package com.condoplus.notificacao.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(schema = "notificacao", value = "notificacao")


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

    public Notificacao() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDestinatarioPessoaId() { return destinatarioPessoaId; }
    public void setDestinatarioPessoaId(UUID destinatarioPessoaId) { this.destinatarioPessoaId = destinatarioPessoaId; }

    public TipoEvento getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEvento tipoEvento) { this.tipoEvento = tipoEvento; }

    public String getEventoOrigemId() { return eventoOrigemId; }
    public void setEventoOrigemId(String eventoOrigemId) { this.eventoOrigemId = eventoOrigemId; }

    public Canal getCanal() { return canal; }
    public void setCanal(Canal canal) { this.canal = canal; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCorpo() { return corpo; }
    public void setCorpo(String corpo) { this.corpo = corpo; }

    public StatusNotificacao getStatus() { return status; }
    public void setStatus(StatusNotificacao status) { this.status = status; }

    public int getTentativas() { return tentativas; }
    public void setTentativas(int tentativas) { this.tentativas = tentativas; }

    public String getUltimoErro() { return ultimoErro; }
    public void setUltimoErro(String ultimoErro) { this.ultimoErro = ultimoErro; }

    public LocalDateTime getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDateTime criadaEm) { this.criadaEm = criadaEm; }

    public LocalDateTime getEnviadaEm() { return enviadaEm; }
    public void setEnviadaEm(LocalDateTime enviadaEm) { this.enviadaEm = enviadaEm; }
}
