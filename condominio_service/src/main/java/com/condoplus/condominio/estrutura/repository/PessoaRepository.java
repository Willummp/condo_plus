package com.condoplus.condominio.estrutura.repository;

import com.condoplus.condominio.estrutura.domain.Pessoa;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface PessoaRepository extends CrudRepository<Pessoa, UUID> {

    
    Optional<Pessoa> findByCredencialId(UUID credencialId);

    
    Optional<Pessoa> findByDocumento(String documento);

    
    boolean existsByDocumento(String documento);

    
    boolean existsByCredencialId(UUID credencialId);
}
