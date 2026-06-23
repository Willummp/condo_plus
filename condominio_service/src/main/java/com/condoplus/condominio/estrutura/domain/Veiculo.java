package com.condoplus.condominio.estrutura.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table(schema = "condominio", value = "veiculo")
@Getter
@Setter
@NoArgsConstructor
public class Veiculo {

    
    @Id
    private UUID id;

    
    @Column("placa")
    private String placa;

    
    @Column("modelo")
    private String modelo;

    
    @Column("cor")
    private String cor;

    
    @Column("unidade_id")
    private AggregateReference<Unidade, UUID> unidadeId;

    
    @Column("ativo")
    private boolean ativo;

    
    public static Veiculo criar(String placa, String modelo, String cor, UUID unidadeId) {
        Veiculo v = new Veiculo();
        v.placa = placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
        v.modelo = modelo;
        v.cor = cor;
        v.unidadeId = AggregateReference.to(unidadeId);
        v.ativo = true;
        return v;
    }

    
    public void desativar() {
        this.ativo = false;
    }
}
