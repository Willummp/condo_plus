package com.condoplus.condominio.estrutura.repository;

import com.condoplus.condominio.estrutura.domain.Veiculo;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de persistência para a entidade {@link Veiculo} (Aggregate Root).
 * 
 * <p>Responsável por fornecer operações de acesso e controle dos dados de veículos cadastrados
 * no condomínio, abstraindo consultas diretas no banco de dados PostgreSQL.
 * 
 * <p>Anotações e conceitos importantes:
 * <ul>
 *   <li>{@code CrudRepository<Veiculo, UUID>} — Interface do Spring Data JDBC para operações CRUD padrão.</li>
 *   <li>{@code @Query} — Utilizada para consultas nativas customizadas de veículos por unidade.</li>
 * </ul>
 */
public interface VeiculoRepository extends CrudRepository<Veiculo, UUID> {

    /**
     * Busca um veículo cadastrado com base em sua placa única.
     * 
     * @param placa Placa do veículo (normalizada).
     * @return Um {@link Optional} contendo o veículo correspondente, ou vazio se não localizado.
     */
    Optional<Veiculo> findByPlaca(String placa);

    /**
     * Verifica se já existe algum veículo cadastrado no sistema com a placa informada.
     * 
     * @param placa Placa a ser verificada.
     * @return {@code true} se o veículo existir, caso contrário {@code false}.
     */
    boolean existsByPlaca(String placa);

    /**
     * Lista todos os veículos ativos que estão vinculados a uma determinada unidade residencial.
     * 
     * @param unidadeId ID exclusivo da unidade residencial.
     * @return Lista de veículos ativos pertencentes à unidade.
     */
    @Query("SELECT * FROM condominio.veiculo WHERE unidade_id = :unidadeId AND ativo = TRUE")
    List<Veiculo> findAtivosByUnidade(UUID unidadeId);
}
