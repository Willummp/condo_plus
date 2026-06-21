package com.condoplus.portaria_service.service;


import com.condoplus.portaria_service.dto.request.NovaEncomendaRequest;
import com.condoplus.portaria_service.dto.request.RetiradaRequest;
import com.condoplus.portaria_service.dto.response.EncomendaResponseDTO;
import com.condoplus.portaria_service.exception.EncomendaJaRetiradaException;
import com.condoplus.portaria_service.exception.EncomendaNaoEncontradaException;
import com.condoplus.portaria_service.messaging.EventoPublicador;
import com.condoplus.portaria_service.model.entities.Encomenda;
import com.condoplus.portaria_service.model.enums.StatusEncomenda;
import com.condoplus.portaria_service.model.enums.TipoEncomenda;
import com.condoplus.portaria_service.repository.EncomendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EncomendaService {

    private final EncomendaRepository encomendaRepository;
    private final EncomendaRedisStore redisStore;
    private final EventoPublicador eventoPublicador;

    @Value("${condoplus.kafka.topics.encomendas-recebidas}")
    private String topicoRecebidas;

    @Value("${condoplus.kafka.topics.encomendas-retiradas}")
    private String topicoRetiradas;

    @Transactional
    public EncomendaResponseDTO receber(NovaEncomendaRequest req, UUID porteiroId) {
        Encomenda encomenda = Encomenda.builder()
                .unidadeId(req.unidadeId())
                .tipo(req.tipo())
                .descricao(req.descricao())
                .codigoRastreio(req.codigoRastreio())
                .status(StatusEncomenda.AGUARDANDO_RETIRADA)
                .porteiroRecebedorId(porteiroId)
                .build();

        Encomenda salva = encomendaRepository.save(encomenda);

        if (req.tipo() == TipoEncomenda.CURTO_PRAZO) {
            redisStore.registrarCurtoPrazo(salva.getUnidadeId(), salva.getId());
        }

        eventoPublicador.publicar(topicoRecebidas, salva.getUnidadeId().toString(), Map.of(
                "eventId",        UUID.randomUUID().toString(),
                "occurredAt",     Instant.now().toString(),
                "encomendaId",    salva.getId().toString(),
                "unidadeId",      salva.getUnidadeId().toString(),
                "tipo",           salva.getTipo().name(),
                "descricao",      salva.getDescricao() != null ? salva.getDescricao() : "",
                "codigoRastreio", salva.getCodigoRastreio() != null ? salva.getCodigoRastreio() : ""
        ));

        log.info("Encomenda recebida. id={} unidadeId={} tipo={}",
                salva.getId(), salva.getUnidadeId(), salva.getTipo());

        return EncomendaResponseDTO.fromEntity(salva);
    }

    @Transactional
    public EncomendaResponseDTO retirar(UUID encomendaId, RetiradaRequest req, UUID porteiroEntregadorId) {
        Encomenda encomenda = encomendaRepository.findById(encomendaId)
                .orElseThrow(() -> new EncomendaNaoEncontradaException(encomendaId));

        if (encomenda.getStatus() != StatusEncomenda.AGUARDANDO_RETIRADA) {
            throw new EncomendaJaRetiradaException(encomendaId, encomenda.getStatus());
        }

        encomenda.marcarComoRetirada(req.retiradoPorPessoaId(), porteiroEntregadorId);
        encomendaRepository.save(encomenda);

        if (encomenda.getTipo() == TipoEncomenda.CURTO_PRAZO) {
            redisStore.removerCurtoPrazo(encomenda.getUnidadeId(), encomenda.getId());
        }

        eventoPublicador.publicar(topicoRetiradas, encomenda.getUnidadeId().toString(), Map.of(
                "eventId",             UUID.randomUUID().toString(),
                "occurredAt",          Instant.now().toString(),
                "encomendaId",         encomenda.getId().toString(),
                "unidadeId",           encomenda.getUnidadeId().toString(),
                "retiradoPorPessoaId", req.retiradoPorPessoaId().toString(),
                "dataRetirada",        encomenda.getDataRetirada().toString()
        ));

        log.info("Encomenda retirada. id={} retiradoPor={}",
                encomenda.getId(), req.retiradoPorPessoaId());

        return EncomendaResponseDTO.fromEntity(encomenda);
    }

    @Transactional(readOnly = true)
    public List<EncomendaResponseDTO> listarPendentesPorUnidade(UUID unidadeId) {
        return encomendaRepository
                .findByUnidadeIdAndStatus(unidadeId, StatusEncomenda.AGUARDANDO_RETIRADA)
                .stream()
                .map(EncomendaResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EncomendaResponseDTO buscar(UUID id) {
        return EncomendaResponseDTO.fromEntity(
                encomendaRepository.findById(id)
                        .orElseThrow(() -> new EncomendaNaoEncontradaException(id)));
    }
}