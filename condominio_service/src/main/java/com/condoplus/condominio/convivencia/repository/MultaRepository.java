package com.condoplus.condominio.convivencia.repository;

import com.condoplus.condominio.convivencia.domain.Multa;
import com.condoplus.condominio.convivencia.domain.StatusMulta;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório de persistência para a entidade Multa (Aggregate Root).
 * 
 * <p>Anotações e conceitos importantes:
 * <ul>
 *   <li>{@code CrudRepository<Multa, UUID>} — Interface base do Spring Data JDBC para operações padrão de CRUD da entidade Multa.</li>
 * </ul>
 */
public interface MultaRepository extends CrudRepository<Multa, UUID> {

    /**
     * Consulta e retorna todas as multas aplicadas a uma determinada unidade do condomínio.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Query} — Executa uma consulta SQL nativa no schema de convivência.</li>
     * </ul>
     * 
     * @param unidadeId ID único (UUID) da unidade condominial.
     * @return Lista das multas encontradas de forma cronológica decrescente.
     */
    @Query("SELECT * FROM condominio.multa WHERE unidade_id = :unidadeId ORDER BY data_aplicacao DESC")
    List<Multa> findByUnidade(UUID unidadeId);

    /**
     * Filtra e retorna multas aplicadas a uma unidade condominial de acordo com o seu status de pagamento.
     * 
     * <p>Anotações do método:
     * <ul>
     *   <li>{@code @Query} — Executa uma consulta SQL nativa contendo filtro condicional no banco.</li>
     * </ul>
     * 
     * @param unidadeId ID único (UUID) da unidade condominial.
     * @param status Status do pagamento da multa (ex: PENDENTE, PAGA).
     * @return Lista de multas encontradas filtradas de forma ordenada por data de vencimento.
     */
    @Query("SELECT * FROM condominio.multa WHERE unidade_id = :unidadeId AND status = :status ORDER BY data_vencimento")
    List<Multa> findByUnidadeEStatus(UUID unidadeId, StatusMulta status);
}
