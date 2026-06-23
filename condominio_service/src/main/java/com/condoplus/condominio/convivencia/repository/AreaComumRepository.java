package com.condoplus.condominio.convivencia.repository;

import com.condoplus.condominio.convivencia.domain.AreaComum;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface AreaComumRepository extends CrudRepository<AreaComum, UUID> {

    
    @Query("SELECT * FROM condominio.area_comum WHERE ativa = TRUE ORDER BY nome")
    List<AreaComum> findAllAtivas();

    
    boolean existsByNome(String nome);
}
