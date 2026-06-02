package com.condoplus.condominio.config;

import com.condoplus.condominio.convivencia.domain.AreaComum;
import com.condoplus.condominio.convivencia.domain.Comunicado;
import com.condoplus.condominio.convivencia.domain.Multa;
import com.condoplus.condominio.convivencia.domain.Reserva;
import com.condoplus.condominio.estrutura.domain.Funcionario;
import com.condoplus.condominio.estrutura.domain.Pessoa;
import com.condoplus.condominio.estrutura.domain.Unidade;
import com.condoplus.condominio.estrutura.domain.Veiculo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;

import java.util.UUID;

/**
 * Configuração do Spring Data JDBC.
 *
 * <p>Problema: Spring Data JDBC não tem @GeneratedValue como JPA.
 * Solução: BeforeConvertCallback roda ANTES de converter o aggregate em SQL,
 * gerando o UUID em Java.
 *
 * <p>Por que gerar o UUID em Java e não no banco (gen_random_uuid())?
 * Se o banco gerasse o UUID, o objeto Java ficaria sem ID após o save().
 * O Spring precisaria de configuração extra para recuperar o valor gerado.
 * Gerar em Java é mais simples e o Spring detecta INSERT vs UPDATE
 * pelo campo @Version (null = novo aggregate = INSERT).
 *
 * <p>Por que um callback por aggregate?
 * O BeforeConvertCallback é tipado — precisamos de um por tipo de aggregate root.
 * No callback de Unidade, geramos também os IDs das Vinculacoes internas,
 * pois elas não têm callback próprio (pertencem ao aggregate).
 */
@Configuration
public class JdbcConfig {

    @Bean
    BeforeConvertCallback<Unidade> unidadeIdGenerator() {
        return aggregate -> {
            if (aggregate.getId() == null) {
                aggregate.setId(UUID.randomUUID());
            }
            // Gera IDs para as vinculações internas do aggregate
            aggregate.getVinculacoes().forEach(v -> {
                if (v.getId() == null) {
                    v.setId(UUID.randomUUID());
                }
            });
            return aggregate;
        };
    }

    @Bean
    BeforeConvertCallback<Pessoa> pessoaIdGenerator() {
        return aggregate -> {
            if (aggregate.getId() == null) {
                aggregate.setId(UUID.randomUUID());
            }
            return aggregate;
        };
    }

    @Bean
    BeforeConvertCallback<Veiculo> veiculoIdGenerator() {
        return aggregate -> {
            if (aggregate.getId() == null) {
                aggregate.setId(UUID.randomUUID());
            }
            return aggregate;
        };
    }

    @Bean
    BeforeConvertCallback<Funcionario> funcionarioIdGenerator() {
        return aggregate -> {
            if (aggregate.getId() == null) {
                aggregate.setId(UUID.randomUUID());
            }
            return aggregate;
        };
    }

    @Bean
    BeforeConvertCallback<Comunicado> comunicadoIdGenerator() {
        return aggregate -> {
            if (aggregate.getId() == null) {
                aggregate.setId(UUID.randomUUID());
            }
            return aggregate;
        };
    }

    @Bean
    BeforeConvertCallback<Multa> multaIdGenerator() {
        return aggregate -> {
            if (aggregate.getId() == null) {
                aggregate.setId(UUID.randomUUID());
            }
            return aggregate;
        };
    }

    @Bean
    BeforeConvertCallback<AreaComum> areaComumIdGenerator() {
        return aggregate -> {
            if (aggregate.getId() == null) {
                aggregate.setId(UUID.randomUUID());
            }
            return aggregate;
        };
    }

    @Bean
    BeforeConvertCallback<Reserva> reservaIdGenerator() {
        return aggregate -> {
            if (aggregate.getId() == null) {
                aggregate.setId(UUID.randomUUID());
            }
            return aggregate;
        };
    }
}
