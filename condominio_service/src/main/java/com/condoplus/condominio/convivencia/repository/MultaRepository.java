package com.condoplus.condominio.convivencia.repository;

import com.condoplus.condominio.convivencia.domain.Multa;
import com.condoplus.condominio.convivencia.domain.StatusMulta;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface MultaRepository extends CrudRepository<Multa, UUID> {

    
    @Query("SELECT * FROM condominio.multa WHERE unidade_id = :unidadeId ORDER BY data_aplicacao DESC")
    List<Multa> findByUnidade(UUID unidadeId);

    
    @Query("SELECT * FROM condominio.multa WHERE unidade_id = :unidadeId AND status = :status ORDER BY data_vencimento")
    List<Multa> findByUnidadeEStatus(UUID unidadeId, StatusMulta status);
}
