package com.condoplus.condominio.estrutura.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * Aggregate Root do bounded context de Estrutura, representando um veículo cadastrado.
 * 
 * <p>A classe {@code Veiculo} gerencia as informações cadastrais dos automóveis permitidos no condomínio.
 * É modelada como um Aggregate Root independente, referenciando a unidade habitacional associada
 * por meio do mecanismo de {@code AggregateReference}.
 * 
 * <p>Mecanismo de {@code AggregateReference}:
 * <ul>
 *   <li>Utiliza {@code AggregateReference<Unidade, UUID>} para referenciar a {@link Unidade} sem acoplar os aggregates em memória.</li>
 *   <li>Em termos de persistência SQL, mapeia para uma coluna do tipo UUID ({@code unidade_id}) na tabela {@code veiculo}.</li>
 * </ul>
 * 
 * <p>Anotações e mapeamentos aplicados:
 * <ul>
 *   <li>{@code @Table("veiculo", schema = "condominio")} — Mapeia esta entidade para a tabela "veiculo" no esquema "condominio" do PostgreSQL.</li>
 *   <li>{@code @Getter} — Geração automática de todos os métodos getters pelo Lombok.</li>
 *   <li>{@code @Setter} — Geração automática de todos os métodos setters pelo Lombok.</li>
 *   <li>{@code @NoArgsConstructor} — Construtor padrão sem argumentos exigido pelo Spring Data JDBC.</li>
 * </ul>
 */
@Table(schema = "condominio", value = "veiculo")
@Getter
@Setter
@NoArgsConstructor
public class Veiculo {

    /**
     * Identificador único universal (UUID) gerado automaticamente para o veículo.
     */
    @Id
    private UUID id;

    /**
     * Placa de identificação regulamentar do veículo (ex: "ABC1D23" ou "XYZ9999").
     */
    @Column("placa")
    private String placa;

    /**
     * Marca e modelo do veículo (ex: "Toyota Corolla", "Honda Civic").
     */
    @Column("modelo")
    private String modelo;

    /**
     * Cor predominante da lataria do veículo (ex: "Preto", "Prata").
     */
    @Column("cor")
    private String cor;

    /**
     * Referência encapsulada ao Aggregate Root {@link Unidade} residencial correspondente à vaga do veículo.
     */
    @Column("unidade_id")
    private AggregateReference<Unidade, UUID> unidadeId;

    /**
     * Flag lógico indicando se o cadastro do veículo permanece ativo para fins de acesso às garagens.
     */
    @Column("ativo")
    private boolean ativo;

    /**
     * Método estático de fábrica para inicializar uma nova instância de Veiculo com placa normalizada.
     * 
     * @param placa Placa do veículo (será normalizada para caixa alta e caracteres alfanuméricos puros).
     * @param modelo Modelo e marca do veículo.
     * @param cor Cor do veículo.
     * @param unidadeId ID da unidade residencial à qual o veículo está atrelado.
     * @return Uma instância configurada de {@link Veiculo} ativa no sistema.
     */
    public static Veiculo criar(String placa, String modelo, String cor, UUID unidadeId) {
        Veiculo v = new Veiculo();
        v.placa = placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
        v.modelo = modelo;
        v.cor = cor;
        v.unidadeId = AggregateReference.to(unidadeId);
        v.ativo = true;
        return v;
    }

    /**
     * Desativa o cadastro do veículo no condomínio, revogando permissões automáticas de tráfego.
     */
    public void desativar() {
        this.ativo = false;
    }
}
