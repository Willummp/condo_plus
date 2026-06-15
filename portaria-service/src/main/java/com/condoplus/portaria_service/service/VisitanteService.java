package com.condoplus.portaria_service.service;

import com.condoplus.portaria_service.dto.CriarVisitanteDTO;
import com.condoplus.portaria_service.dto.RegistrarEntradaVisitanteDTO;
import com.condoplus.portaria_service.dto.VisitanteResponseDTO;
import com.condoplus.portaria_service.mapper.VisitanteMapper;
import com.condoplus.portaria_service.model.entities.RegistroAcesso;
import com.condoplus.portaria_service.model.entities.Visitante;
import com.condoplus.portaria_service.model.enums.StatusVisitante;
import com.condoplus.portaria_service.model.enums.TipoMovimento;
import com.condoplus.portaria_service.model.enums.TipoPessoaAcesso;
import com.condoplus.portaria_service.repository.RegistroAcessoRepository;
import com.condoplus.portaria_service.repository.VisitanteRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitanteService {

    private final VisitanteRepository visitanteRepository;
    private final RegistroAcessoRepository registroAcessoRepository;

    @Transactional
    @CacheEvict(value = "visitantesPorUnidade", key = "#dto.autorizadoParaUnidadeId")
    public VisitanteResponseDTO criarVisitante(CriarVisitanteDTO dto) {

        Visitante visitante = VisitanteMapper.toEntity(dto);

        Visitante salvo = visitanteRepository.save(visitante);

        return VisitanteMapper.toResponse(salvo);
    }

    @Transactional
    public void registrarEntradaVisitante(RegistrarEntradaVisitanteDTO dto) {

        LocalDateTime agora = LocalDateTime.now();

        Visitante visitante = visitanteRepository
                .findAtivosPorDocumento(dto.documento(), StatusVisitante.AUTORIZADO, agora)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Visitante não autorizado"));

        RegistroAcesso registro = RegistroAcesso.builder()
                .tipoPessoa(TipoPessoaAcesso.VISITANTE)
                .visitanteId(visitante.getId())
                .unidadeId(visitante.getAutorizadoParaUnidadeId())
                .tipoMovimento(TipoMovimento.ENTRADA)
                .porteiroId(dto.porteiroId())
                .build();

        registroAcessoRepository.save(registro);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "visitantesPorUnidade", key = "#unidadeId")
    public List<VisitanteResponseDTO> listarPorUnidade(UUID unidadeId) {
        return visitanteRepository.findByAutorizadoParaUnidadeId(unidadeId)
                .stream()
                .map(VisitanteMapper::toResponse)
                .toList();
    }
}