package com.condoplus.condominio.convivencia.repository;

import com.condoplus.condominio.convivencia.domain.Comunicado;
import com.condoplus.condominio.convivencia.domain.PublicoAlvo;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório de persistência para a entidade Comunicado (Aggregate Root).
 * 
 * <p>Anotações e conceitos importantes:
 * <ul>
 *   <li>{@code CrudRepository<Comunicado, UUID>} — Interface base do Spring Data JDBC para operações padrão de CRUD da entidade Comunicado.</li>
 * </ul>
 */
public interface ComunicadoRepository extends CrudRepository<Comunicado, UUID> {

    /**
     * Consulta comunicados recentes de forma ordenada por data e limitada por paginação.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Query} — Executa uma consulta SQL nativa personalizada com limites de paginação.</li>
     * </ul>
     * 
     * @param limit Quantidade máxima de registros a retornar na página.
     * @param offset Quantidade de registros a pular/ignorar no início.
     * @return Lista contendo os comunicados recentes correspondentes ao filtro.
     */
    @Query("SELECT * FROM condominio.comunicado ORDER BY data_publicacao DESC LIMIT :limit OFFSET :offset")
    List<Comunicado> findRecentes(int limit, int offset);

    /**
     * Filtra e recupera comunicados cadastrados de acordo com a sua classificação de público-alvo.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Query} — Executa uma consulta SQL de filtragem no banco.</li>
     * </ul>
     * 
     * @param publicoAlvo Enum correspondente ao grupo de visibilidade.
     * @return Lista dos comunicados encontrados ordenada por publicação decrescente.
     */
    @Query("SELECT * FROM condominio.comunicado WHERE publico_alvo = :publicoAlvo ORDER BY data_publicacao DESC")
    List<Comunicado> findByPublicoAlvo(PublicoAlvo publicoAlvo);
}
