package com.condoplus.condominio.estrutura.domain;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "condominio", value = "vinculacao_escopo")
public record VinculacaoEscopo(@Column("escopo") Escopo escopo) {
}
