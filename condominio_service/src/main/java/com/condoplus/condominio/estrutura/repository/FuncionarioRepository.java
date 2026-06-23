package com.condoplus.condominio.estrutura.repository;

import com.condoplus.condominio.estrutura.domain.CargoFuncionario;
import com.condoplus.condominio.estrutura.domain.Funcionario;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuncionarioRepository extends CrudRepository<Funcionario, UUID> {

    
    Optional<Funcionario> findByPessoaId(UUID pessoaId);

    
    @Query("SELECT * FROM condominio.funcionario WHERE cargo = :cargo AND ativo = TRUE")
    List<Funcionario> findAtivosByCargo(CargoFuncionario cargo);

    
    @Query("SELECT * FROM condominio.funcionario WHERE ativo = TRUE ORDER BY cargo")
    List<Funcionario> findAllAtivos();
}
