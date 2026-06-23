package com.condoplus.portaria_service.repository;

import com.condoplus.portaria_service.model.entities.Visitante;
import com.condoplus.portaria_service.model.enums.StatusVisitante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VisitanteRepository extends JpaRepository<Visitante, UUID> {

    List<Visitante> findByAutorizadoParaUnidadeId(UUID unidadeId);

    @Query("""
            SELECT v FROM Visitante v
            WHERE v.documento = :documento
              AND v.status = :status
              AND v.validadeInicio <= :agora
              AND v.validadeFim >= :agora
            """)
    List<Visitante> findAtivosPorDocumento(
            @Param("documento") String documento,
            @Param("status") StatusVisitante status,
            @Param("agora") LocalDateTime agora);
}