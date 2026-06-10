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

/**
 * Identidade estável da pessoa física no sistema de condomínio.
 * 
 * <p>Responsável por gerenciar os dados fundamentais de moradores, proprietários e funcionários,
 * atuando como a referência unificada de identidade (ID estável) em todo o ecossistema Condo Plus.
 * 
 * <p>Por que a classe {@code Pessoa} é a referência estável em vez de Credencial ou Funcionario?
 * <ul>
 *   <li>Credenciais de acesso podem mudar (e-mail alterado, redefinição de conta), enquanto a identidade física permanece.</li>
 *   <li>Funcionários possuem ciclo de vida limitado (admissão/demissão). Após o desligamento, a entidade histórica de pessoa física continua sendo relevante.</li>
 *   <li>A Pessoa física persiste independentemente de possuir ou não uma credencial de acesso ativa no IAM.</li>
 * </ul>
 * 
 * <p>Anotações e mapeamentos aplicados:
 * <ul>
 *   <li>{@code @Table("pessoa", schema = "condominio")} — Mapeia esta entidade para a tabela "pessoa" no esquema "condominio" do PostgreSQL.</li>
 *   <li>{@code @Getter} — Geração automática de todos os métodos getters pelo Lombok.</li>
 *   <li>{@code @Setter} — Geração automática de todos os métodos setters pelo Lombok.</li>
 *   <li>{@code @NoArgsConstructor} — Construtor padrão sem argumentos exigido pelo Spring Data JDBC para reflexão e instanciação.</li>
 * </ul>
 */
@Table(schema = "condominio", value = "pessoa")
@Getter
@Setter
@NoArgsConstructor
public class Pessoa {

    /**
     * Identificador único universal (UUID) gerado automaticamente para a pessoa física.
     */
    @Id
    private UUID id;

    /**
     * Identificador único de referência da credencial de segurança no microserviço de IAM (iam-service).
     * 
     * <p>Nota arquitetural:
     * É armazenado como {@code UUID} simples e não como {@code AggregateReference}, pois aponta para um
     * aggregate externo que reside em outro banco de dados gerenciado por um microserviço separado.
     */
    @Column("credencial_id")
    private UUID credencialId;

    /**
     * Nome completo da pessoa física.
     */
    @Column("nome_completo")
    private String nomeCompleto;

    /**
     * Número de telefone para contato e emergências.
     */
    @Column("telefone")
    private String telefone;

    /**
     * Documento de identificação nacional exclusivo (CPF) da pessoa física.
     */
    @Column("documento")
    private String documento;

    /**
     * Endereço eletrônico (e-mail) principal utilizado para contatos operacionais e notificações.
     */
    @Column("email_contato")
    private String emailContato;

    /**
     * Data e hora exata em que o cadastro da pessoa física foi inicializado no sistema.
     */
    @CreatedDate
    @Column("criada_em")
    private LocalDateTime criadaEm;

    /**
     * Método de fábrica (Static Factory Method) responsável por inicializar uma nova instância de Pessoa.
     * 
     * @param credencialId ID da credencial correspondente criada no microserviço de IAM.
     * @param nomeCompleto Nome completo do cadastrado.
     * @param documento CPF único da pessoa.
     * @param telefone Telefone de contato.
     * @param emailContato E-mail preferencial de contato.
     * @return Uma instância configurada de {@link Pessoa} pronta para persistência.
     */
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
