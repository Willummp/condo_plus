package com.condoplus.auditoria.repository;

import com.condoplus.auditoria.domain.Anomalia;
import com.condoplus.auditoria.domain.StatusAnomalia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnomaliaRepository extends MongoRepository<Anomalia, String> {

    // Preparados para o C21 (sindico consulta anomalias por status)
    Page<Anomalia> findByStatus(StatusAnomalia status, Pageable pageable);
    Page<Anomalia> findAllByOrderByDetectadaEmDesc(Pageable pageable);
}