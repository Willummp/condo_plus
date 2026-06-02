package com.condoplus.condominio.estrutura.repository;

import com.condoplus.condominio.estrutura.domain.Unidade;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de persistência para a entidade {@link Unidade} (Aggregate Root).
 * 
 * <p>Responsável por fornecer operações de acesso e manipulação das unidades habitacionais
 * e de suas coleções internas de vinculações no banco de dados PostgreSQL.
 * 
 * <p>Anotações e conceitos importantes:
 * <ul>
 *   <li>{@code CrudRepository<Unidade, UUID>} — Interface base do Spring Data JDBC para persistência.</li>
 *   <li>{@code @Query} — Anotação para execução de consultas SQL nativas específicas no banco de dados (Spring Data JDBC não suporta JPQL/HQL).</li>
 *   <li>Carregamento Eager: Qualquer consulta que carregue uma Unidade trará automaticamente suas vinculações (sem suporte a Lazy Loading).</li>
 * </ul>
 */
public interface UnidadeRepository extends CrudRepository<Unidade, UUID> {

    /**
     * Busca uma unidade com base em seu número e identificador do bloco.
     * 
     * @param numero Número identificador da unidade (ex: "101").
     * @param bloco Bloco correspondente (ex: "A").
     * @return Um {@link Optional} contendo a unidade localizada, ou vazio se não encontrada.
     */
    Optional<Unidade> findByNumeroAndBloco(String numero, String bloco);

    /**
     * Verifica a existência de uma unidade registrada utilizando número e bloco como critérios.
     * 
     * @param numero Número identificador da unidade.
     * @param bloco Bloco residencial.
     * @return {@code true} se a unidade já estiver cadastrada, caso contrário {@code false}.
     */
    boolean existsByNumeroAndBloco(String numero, String bloco);

    /**
     * Lista todas as unidades associadas a um bloco residencial específico, ordenadas pelo número.
     * 
     * @param bloco Identificador do bloco desejado.
     * @return Lista das unidades pertencentes ao bloco, em ordem crescente de número.
     */
    @Query("SELECT * FROM condominio.unidade WHERE bloco = :bloco ORDER BY numero")
    List<Unidade> findAllByBloco(String bloco);

    /**
     * Retorna todas as unidades cadastradas no sistema ordenadas por bloco (nulos primeiro) e número.
     * 
     * @return Lista de todas as unidades ordenadas de forma padronizada.
     */
    @Query("SELECT * FROM condominio.unidade ORDER BY bloco NULLS FIRST, numero")
    List<Unidade> findAllOrdered();
}
