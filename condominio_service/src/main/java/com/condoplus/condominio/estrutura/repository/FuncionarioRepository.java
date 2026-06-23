package com.condoplus.condominio.estrutura.repository;

import com.condoplus.condominio.estrutura.domain.CargoFuncionario;
import com.condoplus.condominio.estrutura.domain.Funcionario;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de persistência para a entidade {@link Funcionario} (Aggregate Root).
 * 
 * <p>Responsável por fornecer operações de acesso e controle dos dados de funcionários
 * associados ao condomínio, abstraindo consultas diretas no banco de dados PostgreSQL.
 * 
 * <p>Anotações e conceitos importantes:
 * <ul>
 *   <li>{@code CrudRepository<Funcionario, UUID>} — Interface do Spring Data JDBC para fornecer operações CRUD padrão.</li>
 *   <li>{@code @Query} — Executa consultas SQL nativas personalizadas no esquema condominio.</li>
 * </ul>
 */
public interface FuncionarioRepository extends CrudRepository<Funcionario, UUID> {

    /**
     * Busca um funcionário cadastrado com base no ID exclusivo da pessoa física.
     * 
     * @param pessoaId ID da pessoa física.
     * @return Um {@link Optional} contendo o funcionário, ou vazio se não localizado.
     */
    Optional<Funcionario> findByPessoaId(UUID pessoaId);

    /**
     * Lista todos os funcionários ativos que exercem um determinado cargo corporativo.
     * 
     * @param cargo Cargo ocupacional.
     * @return Lista dos funcionários ativos correspondentes ao cargo.
     */
    @Query("SELECT * FROM condominio.funcionario WHERE cargo = :cargo AND ativo = TRUE")
    List<Funcionario> findAtivosByCargo(CargoFuncionario cargo);

    /**
     * Retorna a lista de todos os funcionários atualmente ativos no condomínio, ordenados pelo cargo.
     * 
     * @return Lista de todos os funcionários ativos ordenados alfabeticamente.
     */
    @Query("SELECT * FROM condominio.funcionario WHERE ativo = TRUE ORDER BY cargo")
    List<Funcionario> findAllAtivos();
}
