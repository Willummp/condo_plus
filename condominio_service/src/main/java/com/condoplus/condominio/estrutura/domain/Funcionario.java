package com.condoplus.condominio.estrutura.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Table(schema = "condominio", value = "funcionario")
@Getter
@Setter
@NoArgsConstructor
public class Funcionario {

    
    @Id
    private UUID id;

    
    @Column("pessoa_id")
    private AggregateReference<Pessoa, UUID> pessoaId;

    
    @Column("cargo")
    private CargoFuncionario cargo;

    
    @Column("data_admissao")
    private LocalDate dataAdmissao;

    
    @Column("data_desligamento")
    private LocalDate dataDesligamento;

    
    @Column("ativo")
    private boolean ativo;

    
    public static Funcionario criar(UUID pessoaId, CargoFuncionario cargo, LocalDate dataAdmissao) {
        Funcionario f = new Funcionario();
        f.pessoaId = AggregateReference.to(pessoaId);
        f.cargo = cargo;
        f.dataAdmissao = dataAdmissao;
        f.ativo = true;
        return f;
    }

    
    public void desligar(LocalDate data) {
        this.dataDesligamento = data;
        this.ativo = false;
    }
}
