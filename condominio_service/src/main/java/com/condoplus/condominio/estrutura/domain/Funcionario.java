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

/**
 * Bounded context de Estrutura, representando o vínculo trabalhista de uma Pessoa com o condomínio.
 * 
 * <p>A classe {@code Funcionario} gerencia as atribuições de cargos operacionais (como porteiros e zeladores).
 * É modelada como um Aggregate Root independente, mapeando a pessoa correspondente através do mecanismo
 * de {@code AggregateReference}.
 * 
 * <p>Mecanismo de Rastreabilidade e Soft Delete:
 * <ul>
 *   <li>Funcionários desligados nunca são excluídos fisicamente do banco de dados para preservar a integridade referencial.</li>
 *   <li>O microserviço de portaria (portaria-service) referencia o funcionário (porteiro) de forma histórica. A exclusão
 *   física quebraria logs de controle de acesso (auditoria).</li>
 *   <li>O desligamento é efetuado via lógica de soft delete, alterando a flag {@code ativo} e definindo a data de desligamento.</li>
 * </ul>
 * 
 * <p>Anotações e mapeamentos aplicados:
 * <ul>
 *   <li>{@code @Table("funcionario", schema = "condominio")} — Mapeia esta entidade para a tabela "funcionario" no esquema "condominio" do PostgreSQL.</li>
 *   <li>{@code @Getter} — Geração automática de todos os métodos getters pelo Lombok.</li>
 *   <li>{@code @Setter} — Geração automática de todos os métodos setters pelo Lombok.</li>
 *   <li>{@code @NoArgsConstructor} — Construtor padrão sem argumentos exigido pelo Spring Data JDBC.</li>
 * </ul>
 */
@Table(schema = "condominio", value = "funcionario")
@Getter
@Setter
@NoArgsConstructor
public class Funcionario {

    /**
     * Identificador único universal (UUID) gerado automaticamente para o funcionário.
     */
    @Id
    private UUID id;

    /**
     * Referência encapsulada ao Aggregate Root {@link Pessoa} correspondente à identidade física do trabalhador.
     */
    @Column("pessoa_id")
    private AggregateReference<Pessoa, UUID> pessoaId;

    /**
     * Cargo corporativo desempenhado pelo funcionário (ex: PORTEIRO, ZELADOR, SINDICO). Mapeado por {@link CargoFuncionario}.
     */
    @Column("cargo")
    private CargoFuncionario cargo;

    /**
     * Data oficial de admissão ou início das atividades operacionais.
     */
    @Column("data_admissao")
    private LocalDate dataAdmissao;

    /**
     * Data oficial de demissão ou encerramento das atividades operacionais. Permanece nula enquanto o funcionário estiver ativo.
     */
    @Column("data_desligamento")
    private LocalDate dataDesligamento;

    /**
     * Flag lógico de status indicando se o funcionário está em pleno exercício das suas funções.
     */
    @Column("ativo")
    private boolean ativo;

    /**
     * Método estático de fábrica para inicializar um novo funcionário ativo na data de admissão.
     * 
     * @param pessoaId Identificador da pessoa física vinculada.
     * @param cargo Cargo ocupacional exercido.
     * @param dataAdmissao Data de início das atividades.
     * @return Uma instância de {@link Funcionario} configurada com status inicial ativo.
     */
    public static Funcionario criar(UUID pessoaId, CargoFuncionario cargo, LocalDate dataAdmissao) {
        Funcionario f = new Funcionario();
        f.pessoaId = AggregateReference.to(pessoaId);
        f.cargo = cargo;
        f.dataAdmissao = dataAdmissao;
        f.ativo = true;
        return f;
    }

    /**
     * Efetua a demissão/desligamento do funcionário de forma lógica, registrando a data final e inativando seu registro.
     * 
     * @param data Data oficial de desligamento das funções operacionais.
     */
    public void desligar(LocalDate data) {
        this.dataDesligamento = data;
        this.ativo = false;
    }
}
