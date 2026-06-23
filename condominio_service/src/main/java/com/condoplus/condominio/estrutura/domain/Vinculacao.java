package com.condoplus.condominio.estrutura.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table(schema = "condominio", value = "vinculacao")
@Getter
@Setter
@NoArgsConstructor
public class Vinculacao {

    
    @Id
    private UUID id;

    
    @Column("pessoa_id")
    private AggregateReference<Pessoa, UUID> pessoaId;

    
    @Column("tipo")
    private TipoVinculacao tipo;

    
    @Column("data_inicio")
    private LocalDate dataInicio;

    
    @Column("data_fim")
    private LocalDate dataFim;

    
    @Column("status")
    private StatusVinculacao status;

    
    @MappedCollection(idColumn = "vinculacao_id")
    private Set<VinculacaoEscopo> escopos = new HashSet<>();

    
    public static Vinculacao criar(UUID pessoaId, TipoVinculacao tipo, LocalDate dataInicio) {
        Vinculacao v = new Vinculacao();
        v.pessoaId = AggregateReference.to(pessoaId);
        v.tipo = tipo;
        v.dataInicio = dataInicio;
        v.status = StatusVinculacao.ATIVA;
        return v;
    }

    
    public void encerrar(LocalDate dataFim) {
        this.dataFim = dataFim;
        this.status = StatusVinculacao.ENCERRADA;
    }

    
    public void atualizarEscopos(Set<Escopo> novos) {
        this.escopos.clear();
        novos.forEach(e -> this.escopos.add(new VinculacaoEscopo(e)));
    }

    
    public Set<Escopo> getEscoposComoEnum() {
        return escopos.stream()
            .map(VinculacaoEscopo::escopo)
            .collect(Collectors.toSet());
    }

    
    public boolean isAtiva() {
        return status == StatusVinculacao.ATIVA;
    }
}
