package com.condoplus.condominio.estrutura.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(schema = "condominio", value = "pessoa")
@Getter
@Setter
@NoArgsConstructor
public class Pessoa {

    
    @Id
    private UUID id;

    
    @Column("credencial_id")
    private UUID credencialId;

    
    @Column("nome_completo")
    private String nomeCompleto;

    
    @Column("telefone")
    private String telefone;

    
    @Column("documento")
    private String documento;

    
    @Column("email_contato")
    private String emailContato;

    
    @CreatedDate
    @Column("criada_em")
    private LocalDateTime criadaEm;

    
    public static Pessoa criar(UUID credencialId, String nomeCompleto,
                               String documento, String telefone, String emailContato) {
        Pessoa p = new Pessoa();
        p.credencialId = credencialId;
        p.nomeCompleto = nomeCompleto;
        p.documento = documento;
        p.telefone = telefone;
        p.emailContato = emailContato;
        return p;
    }
}
