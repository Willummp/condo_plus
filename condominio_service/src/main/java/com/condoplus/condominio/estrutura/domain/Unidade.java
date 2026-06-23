package com.condoplus.condominio.estrutura.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Table(schema = "condominio", value = "unidade")
@Getter
@Setter
@NoArgsConstructor
public class Unidade {

    
    @Id
    private UUID id;

    
    @Column("numero")
    private String numero;

    
    @Column("bloco")
    private String bloco;

    
    @Column("tipo")
    private TipoUnidade tipo;

    
    @Column("ativa")
    private boolean ativa = true;

    
    @CreatedDate
    @Column("criada_em")
    private LocalDateTime criadaEm;

    
    @Version
    private Long versao;

    
    @MappedCollection(idColumn = "unidade_id")
    private Set<Vinculacao> vinculacoes = new HashSet<>();

    
    public static Unidade criar(String numero, String bloco, TipoUnidade tipo) {
        Unidade u = new Unidade();
        u.numero = numero;
        u.bloco = bloco;
        u.tipo = tipo;
        return u;
    }

    
    public void adicionarVinculacao(Vinculacao v) {
        this.vinculacoes.add(v);
    }

    
    public void removerVinculacao(UUID vinculacaoId) {
        this.vinculacoes.removeIf(v -> v.getId() != null && v.getId().equals(vinculacaoId));
    }

    
    public boolean possuiResidenteAtivo() {
        return vinculacoes.stream()
            .filter(v -> v.getStatus() == StatusVinculacao.ATIVA)
            .anyMatch(v -> v.getTipo() == TipoVinculacao.RESIDENTE
                       || v.getTipo() == TipoVinculacao.PROPRIETARIO_RESIDENTE);
    }
}
