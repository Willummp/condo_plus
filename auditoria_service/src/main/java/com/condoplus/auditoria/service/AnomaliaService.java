package com.condoplus.auditoria.service;

import com.condoplus.auditoria.domain.Anomalia;
import com.condoplus.auditoria.domain.StatusAnomalia;
import com.condoplus.auditoria.repository.AnomaliaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * Logica de consulta e triagem de anomalias. Mantem o controller fino,
 * no mesmo padrao do AuditoriaService.
 */
@Service
public class AnomaliaService {

    private final AnomaliaRepository repository;

    public AnomaliaService(AnomaliaRepository repository) {
        this.repository = repository;
    }

    public Page<Anomalia> listar(StatusAnomalia status, Pageable pageable) {
        // Filtro opcional por status; sem filtro, lista todas (mais recentes primeiro).
        if (status != null) {
            return repository.findByStatus(status, pageable);
        }
        return repository.findAllByOrderByDetectadaEmDesc(pageable);
    }

    public Anomalia atualizarStatus(String id, StatusAnomalia novoStatus) {
        Anomalia anomalia = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Anomalia nao encontrada: " + id));
        anomalia.setStatus(novoStatus);
        return repository.save(anomalia);
    }
}