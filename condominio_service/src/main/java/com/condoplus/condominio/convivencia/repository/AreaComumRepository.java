package com.condoplus.condominio.convivencia.repository;

import com.condoplus.condominio.convivencia.domain.AreaComum;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório de persistência para a entidade AreaComum (Aggregate Root).
 * 
 * <p>Anotações e conceitos importantes:
 * <ul>
 *   <li>{@code CrudRepository<AreaComum, UUID>} — Interface base do Spring Data JDBC para operações padrão de CRUD da entidade AreaComum.</li>
 * </ul>
 */
public interface AreaComumRepository extends CrudRepository<AreaComum, UUID> {

    /**
     * Busca todas as áreas comuns registradas no sistema que estejam marcadas com status ativo.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Query} — Executa uma consulta SQL nativa personalizada no schema condominio.</li>
     * </ul>
     * 
     * @return Lista das áreas comuns ativas ordenadas alfabeticamente pelo nome.
     */
    @Query("SELECT * FROM condominio.area_comum WHERE ativa = TRUE ORDER BY nome")
    List<AreaComum> findAllAtivas();

    /**
     * Verifica a existência de uma área comum utilizando seu nome exclusivo como filtro.
     * 
     * @param nome Nome da área comum.
     * @return {@code true} se já houver área cadastrada com o nome informado, caso contrário {@code false}.
     */
    boolean existsByNome(String nome);
}
