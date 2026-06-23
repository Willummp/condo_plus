package com.condoplus.condominio.convivencia.service;

import com.condoplus.condominio.convivencia.domain.Comunicado;
import com.condoplus.condominio.convivencia.domain.PublicoAlvo;
import com.condoplus.condominio.convivencia.dto.ComunicadoResponse;
import com.condoplus.condominio.convivencia.dto.NovoComunicadoRequest;
import com.condoplus.condominio.convivencia.repository.ComunicadoRepository;
import com.condoplus.condominio.event.ComunicadoPublicadoEvent;
import com.condoplus.condominio.producer.CondominioEventProducer;
import com.condoplus.condominio.estrutura.repository.PessoaRepository;
import org.slf4j.MDC;
import com.condoplus.condominio.estrutura.domain.Pessoa;
import com.condoplus.condominio.exception.PessoaNaoEncontradaException;
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
public class ComunicadoService {

    private final ComunicadoRepository comunicadoRepository;
    private final PessoaRepository pessoaRepository;
    private final CondominioEventProducer eventProducer;
    private final Counter comunicadosCounter;

    public ComunicadoService(ComunicadoRepository comunicadoRepository,
                             PessoaRepository pessoaRepository,
                             CondominioEventProducer eventProducer,
                             MeterRegistry meterRegistry) {
        this.comunicadoRepository = comunicadoRepository;
        this.pessoaRepository = pessoaRepository;
        this.eventProducer = eventProducer;
        this.comunicadosCounter = Counter.builder("condoplus.comunicados.publicados")
                .description("Quantidade total de comunicados publicados no condominio")
                .register(meterRegistry);
    }

    
    @Transactional
    public ComunicadoResponse publicar(NovoComunicadoRequest req, UUID autorId) {
        log.info("Publicando comunicado: titulo={}, autorId={}", req.titulo(), autorId);

        Pessoa autor = pessoaRepository.findByCredencialId(autorId)
                .orElseThrow(() -> new PessoaNaoEncontradaException(autorId));

        Comunicado comunicado = new Comunicado();
        comunicado.setTitulo(req.titulo());
        comunicado.setMensagem(req.conteudo());
        comunicado.setPublicoAlvo(req.visibilidade());
        comunicado.setAutorId(AggregateReference.to(autor.getId()));
        comunicado.setDataPublicacao(LocalDateTime.now());

        if (req.visibilidade() == PublicoAlvo.BLOCO_ESPECIFICO) {
            throw new IllegalArgumentException("Para a visibilidade BLOCO_ESPECIFICO, use o endpoint específico fornecendo o bloco destino.");
        }

        Comunicado salvo = comunicadoRepository.save(comunicado);
        this.comunicadosCounter.increment();
        
        eventProducer.publicarComunicado(new ComunicadoPublicadoEvent(
                salvo.getId(),
                salvo.getTitulo(),
                autorId,
                req.visibilidade().name(),
                salvo.getDataPublicacao(),
                MDC.get("correlationId")
        ));

        log.info("Comunicado publicado com sucesso. id={}", salvo.getId());

        return ComunicadoResponse.fromEntity(salvo);
    }

    
    @Transactional
    public ComunicadoResponse publicarParaBloco(NovoComunicadoRequest req, UUID autorId, String bloco) {
        log.info("Publicando comunicado para bloco: titulo={}, bloco={}, autorId={}", req.titulo(), bloco, autorId);

        Pessoa autor = pessoaRepository.findByCredencialId(autorId)
                .orElseThrow(() -> new PessoaNaoEncontradaException(autorId));

        if (bloco == null || bloco.isBlank()) {
            throw new IllegalArgumentException("O bloco destino deve ser fornecido quando visibilidade é BLOCO_ESPECIFICO.");
        }

        Comunicado comunicado = new Comunicado();
        comunicado.setTitulo(req.titulo());
        comunicado.setMensagem(req.conteudo());
        comunicado.setPublicoAlvo(PublicoAlvo.BLOCO_ESPECIFICO);
        comunicado.setBlocoAlvo(bloco.toUpperCase().trim());
        comunicado.setAutorId(AggregateReference.to(autor.getId()));
        comunicado.setDataPublicacao(LocalDateTime.now());

        Comunicado salvo = comunicadoRepository.save(comunicado);
        this.comunicadosCounter.increment();

        eventProducer.publicarComunicado(new ComunicadoPublicadoEvent(
                salvo.getId(),
                salvo.getTitulo(),
                autorId,
                PublicoAlvo.BLOCO_ESPECIFICO.name(),
                salvo.getDataPublicacao(),
                MDC.get("correlationId")
        ));

        log.info("Comunicado para o bloco {} publicado com sucesso. id={}", bloco, salvo.getId());

        return ComunicadoResponse.fromEntity(salvo);
    }

    
    @Transactional(readOnly = true)
    public ComunicadoResponse buscarPorId(UUID id) {
        return comunicadoRepository.findById(id)
                .map(ComunicadoResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Comunicado não encontrado com o ID: " + id));
    }

    
    @Transactional(readOnly = true)
    public List<ComunicadoResponse> listarTodos(int page, int size) {
        int offset = page * size;
        return comunicadoRepository.findRecentes(size, offset).stream()
                .map(ComunicadoResponse::fromEntity)
                .toList();
    }

    
    @Transactional(readOnly = true)
    public List<ComunicadoResponse> listarPorPublicoAlvo(PublicoAlvo publicoAlvo) {
        return comunicadoRepository.findByPublicoAlvo(publicoAlvo).stream()
                .map(ComunicadoResponse::fromEntity)
                .toList();
    }

    
    @Transactional
    public void remover(UUID id) {
        log.info("Removendo comunicado: {}", id);
        if (!comunicadoRepository.existsById(id)) {
            throw new RuntimeException("Comunicado não encontrado com o ID: " + id);
        }
        comunicadoRepository.deleteById(id);
    }
}
