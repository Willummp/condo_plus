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

/**
 * Entidade interna (Child Entity) pertencente ao Aggregate de {@link Unidade}.
 * 
 * <p>A classe {@code Vinculacao} representa o relacionamento de uma pessoa física com uma unidade habitacional
 * (por exemplo: inquilino, proprietário, morador). Ela não possui ciclo de vida próprio e é gerenciada
 * integralmente pelo Aggregate Root {@code Unidade}. Por este motivo, não possui repositório próprio.
 * 
 * <p>Mecanismo de {@code AggregateReference}:
 * <ul>
 *   <li>Utiliza {@code AggregateReference<Pessoa, UUID>} para referenciar o Aggregate Root {@link Pessoa} sem acoplá-lo diretamente em memória.</li>
 *   <li>Isto respeita as fronteiras transacionais do DDD e do Spring Data JDBC, mapeando em banco uma coluna do tipo UUID ({@code pessoa_id}).</li>
 *   <li>Para carregar a pessoa correspondente, deve-se realizar uma consulta explícita via {@code PessoaRepository.findById(uuid)}.</li>
 * </ul>
 * 
 * <p>Anotações e mapeamentos aplicados:
 * <ul>
 *   <li>{@code @Table("vinculacao", schema = "condominio")} — Mapeia esta entidade para a tabela "vinculacao" no esquema "condominio" do PostgreSQL.</li>
 *   <li>{@code @Getter} — Geração automática de todos os métodos getters pelo Lombok.</li>
 *   <li>{@code @Setter} — Geração automática de todos os métodos setters pelo Lombok.</li>
 *   <li>{@code @NoArgsConstructor} — Construtor padrão sem argumentos exigido para instanciação refletiva.</li>
 * </ul>
 */
@Table(schema = "condominio", value = "vinculacao")
@Getter
@Setter
@NoArgsConstructor
public class Vinculacao {

    /**
     * Identificador único universal (UUID) gerado automaticamente para a vinculação.
     */
    @Id
    private UUID id;

    /**
     * Referência encapsulada para o Aggregate Root {@link Pessoa}.
     * 
     * <p>Evita carregamento ansioso e respeita os limites conceituais do aggregate de Pessoa.
     */
    @Column("pessoa_id")
    private AggregateReference<Pessoa, UUID> pessoaId;

    /**
     * Tipo do vínculo estabelecido (ex: PROPRIETARIO, INQUILINO, MORADOR, etc.). Mapeado por {@link TipoVinculacao}.
     */
    @Column("tipo")
    private TipoVinculacao tipo;

    /**
     * Data de início do vínculo da pessoa com a unidade residencial.
     */
    @Column("data_inicio")
    private LocalDate dataInicio;

    /**
     * Data de encerramento do vínculo (caso aplicável). Permanece nula para vínculos ativos por tempo indeterminado.
     */
    @Column("data_fim")
    private LocalDate dataFim;

    /**
     * Status atual do vínculo (ex: ATIVA, ENCERRADA). Mapeado por {@link StatusVinculacao}.
     */
    @Column("status")
    private StatusVinculacao status;

    /**
     * Coleção interna de escopos associados a este vínculo (ex: SOCIAL, LEGAL, FINANCEIRO).
     * 
     * <p>Mapeado com {@code @MappedCollection} como uma relação de tabelas vinculadas (vinculacao_escopo).
     * Os escopos não devem ser alterados diretamente; são recalculados dinamicamente via {@code EscopoDerivacaoService}.
     */
    @MappedCollection(idColumn = "vinculacao_id")
    private Set<VinculacaoEscopo> escopos = new HashSet<>();

    /**
     * Método estático de fábrica para inicializar uma vinculação ativa.
     * 
     * @param pessoaId Identificador único da pessoa associada.
     * @param tipo Tipo de vínculo estabelecido.
     * @param dataInicio Data de início da vigência do vínculo.
     * @return Uma instância configurada de {@link Vinculacao} com status inicial ATIVA.
     */
    public static Vinculacao criar(UUID pessoaId, TipoVinculacao tipo, LocalDate dataInicio) {
        Vinculacao v = new Vinculacao();
        v.pessoaId = AggregateReference.to(pessoaId);
        v.tipo = tipo;
        v.dataInicio = dataInicio;
        v.status = StatusVinculacao.ATIVA;
        return v;
    }

    /**
     * Finaliza o vínculo atual, definindo a data de encerramento e atualizando o status para ENCERRADA.
     * 
     * @param dataFim Data oficial de encerramento do vínculo.
     */
    public void encerrar(LocalDate dataFim) {
        this.dataFim = dataFim;
        this.status = StatusVinculacao.ENCERRADA;
    }

    /**
     * Atualiza a coleção de escopos de privilégios desta vinculação.
     * 
     * <p>Esta operação limpa os escopos antigos e insere os novos informados. Deve ser invocada pelo 
     * {@code EscopoDerivacaoService} sempre que houver alterações nas regras de negócio da unidade.
     * 
     * @param novos Conjunto contendo os novos escopos aplicáveis à vinculação.
     */
    public void atualizarEscopos(Set<Escopo> novos) {
        this.escopos.clear();
        novos.forEach(e -> this.escopos.add(new VinculacaoEscopo(e)));
    }

    /**
     * Método utilitário para converter as entidades internas {@link VinculacaoEscopo} em enums puros {@link Escopo}.
     * 
     * @return Conjunto contendo os enums de escopos correspondentes ativos na vinculação.
     */
    public Set<Escopo> getEscoposComoEnum() {
        return escopos.stream()
            .map(VinculacaoEscopo::escopo)
            .collect(Collectors.toSet());
    }

    /**
     * Verifica se o vínculo atual está com status ativo.
     * 
     * @return {@code true} se o status for ATIVA, {@code false} caso contrário.
     */
    public boolean isAtiva() {
        return status == StatusVinculacao.ATIVA;
    }
}
