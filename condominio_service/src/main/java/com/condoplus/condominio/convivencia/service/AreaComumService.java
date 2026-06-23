package com.condoplus.condominio.convivencia.service;

import com.condoplus.condominio.convivencia.domain.AreaComum;
import com.condoplus.condominio.convivencia.dto.AreaComumResponse;
import com.condoplus.condominio.convivencia.dto.NovaAreaComumRequest;
import com.condoplus.condominio.convivencia.repository.AreaComumRepository;
import com.condoplus.condominio.exception.AreaComumNaoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class AreaComumService {

    private final AreaComumRepository areaComumRepository;

    
    @Transactional
    public AreaComumResponse cadastrar(NovaAreaComumRequest req) {
        log.info("Cadastrando nova área comum com nome: {}", req.nome());

        if (areaComumRepository.existsByNome(req.nome())) {
            throw new RuntimeException("Já existe uma área comum cadastrada com o nome: " + req.nome());
        }

        AreaComum area = new AreaComum();
        area.setNome(req.nome());
        area.setCapacidadeMaxima(req.capacidadeMaxima());
        area.setValorReserva(req.valorReserva());
        area.setRegras(req.regras());
        area.setAtiva(true);

        AreaComum salva = areaComumRepository.save(area);
        log.info("Área comum cadastrada com sucesso. id={}", salva.getId());

        return AreaComumResponse.fromEntity(salva);
    }

    
    @Transactional(readOnly = true)
    public AreaComumResponse buscarPorId(UUID id) {
        return areaComumRepository.findById(id)
                .map(AreaComumResponse::fromEntity)
                .orElseThrow(() -> new AreaComumNaoEncontradaException(id));
    }

    
    @Transactional(readOnly = true)
    public List<AreaComumResponse> listarTodas(boolean apenasAtivas) {
        if (apenasAtivas) {
            return areaComumRepository.findAllAtivas().stream()
                    .map(AreaComumResponse::fromEntity)
                    .toList();
        }
        return StreamSupport.stream(areaComumRepository.findAll().spliterator(), false)
                .map(AreaComumResponse::fromEntity)
                .toList();
    }

    
    @Transactional
    public AreaComumResponse atualizar(UUID id, NovaAreaComumRequest req) {
        log.info("Atualizando área comum: {}", id);
        AreaComum area = areaComumRepository.findById(id)
                .orElseThrow(() -> new AreaComumNaoEncontradaException(id));

        area.setNome(req.nome());
        area.setCapacidadeMaxima(req.capacidadeMaxima());
        area.setValorReserva(req.valorReserva());
        area.setRegras(req.regras());

        AreaComum salva = areaComumRepository.save(area);
        log.info("Área comum atualizada com sucesso. id={}", id);

        return AreaComumResponse.fromEntity(salva);
    }

    
    @Transactional
    public void desativar(UUID id) {
        log.info("Desativando área comum: {}", id);
        AreaComum area = areaComumRepository.findById(id)
                .orElseThrow(() -> new AreaComumNaoEncontradaException(id));
        
        area.setAtiva(false);
        areaComumRepository.save(area);
        log.info("Área comum desativada com sucesso. id={}", id);
    }

    
    @Transactional
    public void ativar(UUID id) {
        log.info("Ativando área comum: {}", id);
        AreaComum area = areaComumRepository.findById(id)
                .orElseThrow(() -> new AreaComumNaoEncontradaException(id));
        
        area.setAtiva(true);
        areaComumRepository.save(area);
        log.info("Área comum ativada com sucesso. id={}", id);
    }
}
