package com.condoplus.portaria_service.repository;

import com.condoplus.portaria_service.model.entities.Encomenda;
import com.condoplus.portaria_service.model.enums.StatusEncomenda;
import com.condoplus.portaria_service.model.enums.TipoEncomenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EncomendaRepository extends JpaRepository<Encomenda, UUID> {
    List<Encomenda> findByUnidadeIdAndStatus(UUID unidadeId, StatusEncomenda status);
    List<Encomenda> findByTipoAndStatus(TipoEncomenda tipo, StatusEncomenda status);
}