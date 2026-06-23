package com.condoplus.condominio.estrutura.repository;

import com.condoplus.condominio.estrutura.domain.Veiculo;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoRepository extends CrudRepository<Veiculo, UUID> {

    
    Optional<Veiculo> findByPlaca(String placa);

    
    boolean existsByPlaca(String placa);

    
    @Query("SELECT * FROM condominio.veiculo WHERE unidade_id = :unidadeId AND ativo = TRUE")
    List<Veiculo> findAtivosByUnidade(UUID unidadeId);
}
