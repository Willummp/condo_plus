package com.condoplus.portaria_service.service;

import com.condoplus.portaria_service.dto.CriarEncomendaDTO;
import com.condoplus.portaria_service.dto.EncomendaResponseDTO;
import com.condoplus.portaria_service.dto.RetirarEncomendaDTO;
import com.condoplus.portaria_service.mapper.EncomendaMapper;
import com.condoplus.portaria_service.model.entities.Encomenda;
import com.condoplus.portaria_service.model.enums.StatusEncomenda;
import com.condoplus.portaria_service.model.enums.TipoEncomenda;
import com.condoplus.portaria_service.repository.EncomendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EncomendaService {

    private final EncomendaRepository encomendaRepository;
    private final RedisService redisService;

    private static final String KEY_PREFIX = "encomenda:";

    @Transactional
    @CacheEvict(value = "encomendasPendentes", key = "#dto.unidadeId")
    public EncomendaResponseDTO registrarEncomenda(CriarEncomendaDTO dto) {

        Encomenda encomenda = EncomendaMapper.toEntity(dto);

        Encomenda salva = encomendaRepository.save(encomenda);

        Duration ttl = calcularTTL(salva.getTipo());

        redisService.salvarComTTL(KEY_PREFIX + salva.getId(), ttl);

        return EncomendaMapper.toResponse(salva);
    }

    @Transactional
    public void retirarEncomenda(RetirarEncomendaDTO dto) {

        Encomenda encomenda = encomendaRepository.findById(dto.encomendaId())
                .orElseThrow(() -> new IllegalArgumentException("Encomenda não encontrada"));

        String key = KEY_PREFIX + encomenda.getId();

        if (!redisService.existe(key) || encomenda.estaExpirada(LocalDateTime.now())) {
            encomenda.marcarComoExpirada();
            throw new IllegalStateException("Encomenda expirada");
        }

        encomenda.marcarComoRetirada(dto.retiradoPorPessoaId(), dto.porteiroId());

        encomendaRepository.save(encomenda);

        redisService.deletar(key);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "encomendasPendentes", key = "#unidadeId")
    public List<EncomendaResponseDTO> listarPendentesPorUnidade(UUID unidadeId) {
        return encomendaRepository
                .findByUnidadeIdAndStatus(unidadeId, StatusEncomenda.AGUARDANDO_RETIRADA)
                .stream()
                .map(EncomendaMapper::toResponse)
                .toList();
    }

    private Duration calcularTTL(TipoEncomenda tipo) {
        return switch (tipo) {
            case CURTO_PRAZO -> Duration.ofHours(2);
            case MEDIO_PRAZO -> Duration.ofDays(7);
            case LONGO_PRAZO -> Duration.ofDays(30);
        };
    }
}