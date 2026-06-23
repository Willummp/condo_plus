package com.condoplus.notificacao.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(schema = "notificacao", value = "preferencia_notificacao")
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

    public PreferenciaNotificacao() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPessoaId() { return pessoaId; }
    public void setPessoaId(UUID pessoaId) { this.pessoaId = pessoaId; }

    public TipoEvento getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEvento tipoEvento) { this.tipoEvento = tipoEvento; }

    public Canal getCanal() { return canal; }
    public void setCanal(Canal canal) { this.canal = canal; }

    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }

    public LocalDateTime getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDateTime criadaEm) { this.criadaEm = criadaEm; }

    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public void setAtualizadaEm(LocalDateTime atualizadaEm) { this.atualizadaEm = atualizadaEm; }
}
