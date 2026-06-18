package com.condoplus.condominio.estrutura.repository;

import com.condoplus.condominio.estrutura.domain.Pessoa;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de persistência para a entidade {@link Pessoa} (Aggregate Root).
 * 
 * <p>Responsável por fornecer operações de acesso aos dados de pessoas físicas cadastradas,
 * abstraindo consultas SQL diretas no esquema condominio do banco de dados PostgreSQL.
 * 
 * <p>Anotações e conceitos importantes:
 * <ul>
 *   <li>{@code CrudRepository<Pessoa, UUID>} — Interface base do Spring Data JDBC para fornecer operações CRUD padrão de forma automática.</li>
 * </ul>
 */
public interface PessoaRepository extends CrudRepository<Pessoa, UUID> {

    /**
     * Busca uma Pessoa física com base no identificador de sua credencial de segurança do IAM.
     * 
     * @param credencialId ID único da credencial associada no microserviço de IAM.
     * @return Um {@link Optional} contendo a pessoa correspondente, ou vazio se não localizada.
     */
    Optional<Pessoa> findByCredencialId(UUID credencialId);

    /**
     * Busca uma Pessoa física utilizando seu número de CPF/documento exclusivo.
     * 
     * @param documento CPF cadastrado da pessoa.
     * @return Um {@link Optional} contendo a pessoa correspondente, ou vazio se não localizada.
     */
    Optional<Pessoa> findByDocumento(String documento);

    /**
     * Verifica a existência de uma pessoa utilizando seu CPF exclusivo como filtro.
     * 
     * @param documento CPF que se deseja verificar.
     * @return {@code true} se já houver uma pessoa cadastrada com esse documento, caso contrário {@code false}.
     */
    boolean existsByDocumento(String documento);

    /**
     * Verifica a existência de uma pessoa vinculada a uma credencial de segurança específica do IAM.
     * 
     * @param credencialId ID da credencial associada.
     * @return {@code true} se a credencial informada já possuir uma pessoa física vinculada, caso contrário {@code false}.
     */
    boolean existsByCredencialId(UUID credencialId);
}
