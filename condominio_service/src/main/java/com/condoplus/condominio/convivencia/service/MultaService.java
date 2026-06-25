package com.condoplus.condominio.convivencia.service;

import com.condoplus.condominio.convivencia.domain.Multa;
import com.condoplus.condominio.convivencia.domain.StatusMulta;
import com.condoplus.condominio.convivencia.dto.MultaResponse;
import com.condoplus.condominio.convivencia.dto.NovaMultaRequest;
import com.condoplus.condominio.convivencia.repository.MultaRepository;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import com.condoplus.condominio.estrutura.repository.UnidadeRepository;
import com.condoplus.condominio.exception.UnidadeNaoEncontradaException;
import com.condoplus.condominio.event.MultaAplicadaEvent;
import com.condoplus.condominio.producer.CondominioEventProducer;
import org.slf4j.MDC;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class MultaService {

    private final MultaRepository multaRepository;
    private final UnidadeRepository unidadeRepository;
    private final PessoaRepository pessoaRepository;
    private final CondominioEventProducer eventProducer;
    private final Counter multasCounter;

    public MultaService(MultaRepository multaRepository,
                        UnidadeRepository unidadeRepository,
                        PessoaRepository pessoaRepository,
                        CondominioEventProducer eventProducer,
                        MeterRegistry meterRegistry) {
        this.multaRepository = multaRepository;
        this.unidadeRepository = unidadeRepository;
        this.pessoaRepository = pessoaRepository;
        this.eventProducer = eventProducer;
        this.multasCounter = Counter.builder("condoplus.multas.aplicadas")
                .description("Quantidade total de multas aplicadas no condominio")
                .register(meterRegistry);
    }

    
    @Transactional
    public MultaResponse aplicar(NovaMultaRequest req, UUID aplicadorId) {
        log.info("Aplicando multa: unidadeId={}, valor={}, aplicadorId={}", req.unidadeId(), req.valor(), aplicadorId);

        if (!unidadeRepository.existsById(req.unidadeId())) {
            throw new UnidadeNaoEncontradaException(req.unidadeId());
        }

        // ADMIN do sistema pode nao ter Pessoa no condominio-service (criado apenas no IAM via seed)
        UUID pessoaId = pessoaRepository.findByCredencialId(aplicadorId)
                .map(p -> p.getId())
                .orElse(null);

        Multa multa = new Multa();
        multa.setUnidadeId(AggregateReference.to(req.unidadeId()));
        multa.setValor(req.valor());
        multa.setMotivo(req.motivo());
        multa.setCategoria(req.categoria());
        multa.setAnexoEvidenciaUrl(req.anexoEvidenciaUrl());
        multa.setDataAplicacao(LocalDateTime.now());
        multa.setDataVencimento(req.dataVencimento());
        multa.setStatus(StatusMulta.PENDENTE);
        multa.setAplicadaPorId(pessoaId != null ? AggregateReference.to(pessoaId) : null);

        Multa salva = multaRepository.save(multa);
        this.multasCounter.increment();

        eventProducer.publicarMulta(new MultaAplicadaEvent(
                salva.getId(),
                req.unidadeId(),
                aplicadorId,
                req.motivo(),
                req.valor(),
                salva.getDataAplicacao(),
                MDC.get("correlationId")
        ));

        log.info("Multa aplicada com sucesso. id={}", salva.getId());

        return MultaResponse.fromEntity(salva);
    }

    
    @Transactional(readOnly = true)
    public MultaResponse buscarPorId(UUID id) {
        return multaRepository.findById(id)
                .map(MultaResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Multa não encontrada com o ID: " + id));
    }

    
    @Transactional(readOnly = true)
    public List<MultaResponse> listarTodas() {
        return StreamSupport.stream(multaRepository.findAll().spliterator(), false)
                .map(MultaResponse::fromEntity)
                .toList();
    }

    
    @Transactional(readOnly = true)
    public List<MultaResponse> listarPorUnidade(UUID unidadeId, StatusMulta status) {
        if (!unidadeRepository.existsById(unidadeId)) {
            throw new UnidadeNaoEncontradaException(unidadeId);
        }
        if (status != null) {
            return multaRepository.findByUnidadeEStatus(unidadeId, status).stream()
                    .map(MultaResponse::fromEntity)
                    .toList();
        }
        return multaRepository.findByUnidade(unidadeId).stream()
                .map(MultaResponse::fromEntity)
                .toList();
    }

    
    @Transactional
    public MultaResponse atualizarStatus(UUID id, StatusMulta status) {
        log.info("Atualizando status da multa: id={}, novoStatus={}", id, status);
        Multa multa = multaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Multa não encontrada com o ID: " + id));
        
        multa.setStatus(status);
        Multa salva = multaRepository.save(multa);
        log.info("Status da multa atualizado com sucesso. id={}", id);
        
        return MultaResponse.fromEntity(salva);
    }
}
