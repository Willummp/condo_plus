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

/**
 * Aggregate Root do bounded context de Estrutura, representando uma unidade física (apartamento ou casa).
 * 
 * <p>A classe {@code Unidade} é modelada como a raiz agregadora de vinculações. Ela existe de forma 
 * autônoma, independentemente das pessoas que a ocupam (podendo estar vazia, ocupada por inquilinos, 
 * ou possuir múltiplos proprietários vinculados).
 * 
 * <p>Nota arquitetural importante:
 * Todas as anotações de persistência deste arquivo pertencem ao ecossistema do Spring Data Relational
 * ({@code org.springframework.data.relational.core.mapping}), e NÃO ao JPA/Hibernate. A mistura de ambas
 * anotações impede a execução adequada do mapeamento objeto-relacional.
 * 
 * <p>Anotações e mapeamentos aplicados:
 * <ul>
 *   <li>{@code @Table("unidade", schema = "condominio")} — Mapeia esta entidade para a tabela "unidade" no esquema "condominio" do PostgreSQL.</li>
 *   <li>{@code @Getter} — Geração automática de todos os métodos getters pelo Lombok.</li>
 *   <li>{@code @Setter} — Geração automática de todos os métodos setters pelo Lombok.</li>
 *   <li>{@code @NoArgsConstructor} — Construtor padrão sem argumentos exigido pelo Spring Data JDBC.</li>
 * </ul>
 */
@Table(schema = "condominio", value = "unidade")
@Getter
@Setter
@NoArgsConstructor
public class Unidade {

    /**
     * Identificador único universal (UUID) gerado automaticamente para a unidade residencial.
     */
    @Id
    private UUID id;

    /**
     * Número identificador da unidade (ex: "101", "12").
     */
    @Column("numero")
    private String numero;

    /**
     * Bloco residencial ao qual a unidade pertence (ex: "A", "Torre 2").
     */
    @Column("bloco")
    private String bloco;

    /**
     * Tipo de habitação residencial (ex: APARTAMENTO, CASA). Mapeado a partir de {@link TipoUnidade}.
     */
    @Column("tipo")
    private TipoUnidade tipo;

    /**
     * Flag lógico que indica se a unidade está ativa no condomínio para fins de faturamento e reservas.
     */
    @Column("ativa")
    private boolean ativa = true;

    /**
     * Data e hora exata em que a unidade residencial foi registrada no sistema.
     */
    @CreatedDate
    @Column("criada_em")
    private LocalDateTime criadaEm;

    /**
     * Atributo para controle de concorrência otimista.
     * 
     * <p>Anotações do campo:
     * <ul>
     *   <li>{@code @Version} — Habilita o controle de concorrência otimista do Spring Data. Cada alteração 
     *   incrementa a versão no banco de dados. Caso dois requests tentem alterar simultaneamente a mesma 
     *   unidade residencial, o Spring Data JDBC lançará uma {@code OptimisticLockingFailureException} para 
     *   evitar sobrescritas acidentais (Lost Update).</li>
     * </ul>
     */
    @Version
    private Long versao;

    /**
     * Vinculações de pessoas físicas associadas a esta unidade residencial.
     * 
     * <p>Anotações do campo:
     * <ul>
     *   <li>{@code @MappedCollection(idColumn = "unidade_id")} — Indica que a classe {@link Vinculacao} é uma entidade 
     *   estritamente interna (Child Element) deste aggregate. A persistência e a remoção das vinculações são delegadas 
     *   diretamente a este Aggregate Root. Por este motivo, {@link Vinculacao} não possui um repositório próprio na aplicação.</li>
     * </ul>
     */
    @MappedCollection(idColumn = "unidade_id")
    private Set<Vinculacao> vinculacoes = new HashSet<>();

    /**
     * Método estático de fábrica para inicialização de uma nova instância de Unidade.
     * 
     * @param numero Número da unidade.
     * @param bloco Bloco da unidade.
     * @param tipo Tipo de habitação (APARTAMENTO/CASA).
     * @return Uma instância configurada de {@link Unidade}.
     */
    public static Unidade criar(String numero, String bloco, TipoUnidade tipo) {
        Unidade u = new Unidade();
        u.numero = numero;
        u.bloco = bloco;
        u.tipo = tipo;
        return u;
    }

    /**
     * Adiciona uma vinculação à coleção interna da unidade residencial.
     * 
     * <p>Para persistir esta modificação no banco de dados, o repositório {@code UnidadeRepository#save(Unidade)} 
     * correspondente deve ser chamado subsequentemente.
     * 
     * @param v Nova instância de {@link Vinculacao} a ser agregada à unidade.
     */
    public void adicionarVinculacao(Vinculacao v) {
        this.vinculacoes.add(v);
    }

    /**
     * Remove uma vinculação de pessoa da unidade residencial a partir de seu identificador único.
     * 
     * <p>Para persistir esta exclusão no banco de dados, o repositório {@code UnidadeRepository#save(Unidade)} 
     * correspondente deve ser chamado subsequentemente.
     * 
     * @param vinculacaoId ID exclusivo da vinculação a ser removida.
     */
    public void removerVinculacao(UUID vinculacaoId) {
        this.vinculacoes.removeIf(v -> v.getId() != null && v.getId().equals(vinculacaoId));
    }

    /**
     * Analisa as vinculações ativas da unidade para determinar se existe algum morador ou proprietário residente ativo.
     * 
     * <p>Esta verificação de negócio é essencial para o microserviço e é consumida por serviços como 
     * {@code EscopoDerivacaoService} para computar as regras de acesso e permissões.
     * 
     * @return {@code true} se houver pelo menos um morador ou proprietário residente ativo na unidade, {@code false} caso contrário.
     */
    public boolean possuiResidenteAtivo() {
        return vinculacoes.stream()
            .filter(v -> v.getStatus() == StatusVinculacao.ATIVA)
            .anyMatch(v -> v.getTipo() == TipoVinculacao.RESIDENTE
                       || v.getTipo() == TipoVinculacao.PROPRIETARIO_RESIDENTE);
    }
}
