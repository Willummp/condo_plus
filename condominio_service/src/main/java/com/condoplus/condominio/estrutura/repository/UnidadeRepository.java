package com.condoplus.condominio.estrutura.repository;

import com.condoplus.condominio.estrutura.domain.Unidade;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnidadeRepository extends CrudRepository<Unidade, UUID> {

    
    Optional<Unidade> findByNumeroAndBloco(String numero, String bloco);

    
    boolean existsByNumeroAndBloco(String numero, String bloco);

    
    @Query("SELECT * FROM condominio.unidade WHERE bloco = :bloco ORDER BY numero")
    List<Unidade> findAllByBloco(String bloco);

    
    @Query("SELECT * FROM condominio.unidade ORDER BY bloco NULLS FIRST, numero")
    List<Unidade> findAllOrdered();

    
    @Query("SELECT u.* FROM condominio.unidade u JOIN condominio.vinculacao v ON v.unidade_id = u.id WHERE v.id = :vinculacaoId")
    Optional<Unidade> findByVinculacaoId(UUID vinculacaoId);
}
